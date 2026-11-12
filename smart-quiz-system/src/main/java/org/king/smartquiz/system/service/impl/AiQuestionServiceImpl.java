package org.king.smartquiz.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.king.smartquiz.common.core.ResultCode;
import org.king.smartquiz.common.exception.BusinessException;
import org.king.smartquiz.framework.ai.AiClient;
import org.king.smartquiz.framework.ai.AiJsonUtil;
import org.king.smartquiz.framework.ai.AiProperties;
import org.king.smartquiz.framework.security.UserContext;
import org.king.smartquiz.system.dto.request.AiGenerateDTO;
import org.king.smartquiz.system.entity.Question;
import org.king.smartquiz.system.mapper.QuestionMapper;
import org.king.smartquiz.system.service.AiQuestionService;
import org.king.smartquiz.system.vo.QuestionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 智能出题服务实现。
 * 流程：构建 Prompt → 调用大模型 → 提取并解析 JSON（失败重试）→ 映射为题目 → 按需入库。
 */
@Slf4j
@Service
public class AiQuestionServiceImpl implements AiQuestionService {

    private static final String SYSTEM_PROMPT =
            "你是一名资深命题专家，擅长依据知识点编写高质量、无歧义的试题。" +
            "你只输出 JSON，绝不输出任何解释性文字、前后缀或 Markdown 代码块标记。";

    private final AiClient aiClient;
    private final AiProperties aiProperties;
    private final QuestionMapper questionMapper;

    public AiQuestionServiceImpl(AiClient aiClient, AiProperties aiProperties, QuestionMapper questionMapper) {
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
        this.questionMapper = questionMapper;
    }

    @Override
    public List<QuestionVO> generate(AiGenerateDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        int difficulty = dto.getDifficulty() == null ? 2 : dto.getDifficulty();
        int score = dto.getScore() == null ? 5 : dto.getScore();
        String userPrompt = buildUserPrompt(dto, difficulty);

        JSONArray questions = callAndParse(userPrompt);

        boolean save = Boolean.TRUE.equals(dto.getSave());
        List<QuestionVO> result = new ArrayList<>();
        for (Object obj : questions) {
            JSONObject item = (JSONObject) obj;
            Question q = new Question();
            q.setUserId(userId);
            q.setSubject(dto.getSubject());
            q.setKnowledgePoint(dto.getKnowledgePoint());
            q.setType(dto.getType());
            q.setDifficulty(difficulty);
            q.setTitle(item.getStr("title"));
            q.setOptions(normalizeOptions(item.get("options")));
            q.setAnswer(item.getStr("answer"));
            q.setAnalysis(item.getStr("analysis"));
            q.setScore(score);
            q.setSource(1);

            if (StrUtil.isBlank(q.getTitle()) || StrUtil.isBlank(q.getAnswer())) {
                log.warn("跳过缺少题干或答案的 AI 题目: {}", item);
                continue;
            }
            if (save) {
                questionMapper.insert(q);
            }
            QuestionVO vo = new QuestionVO();
            BeanUtils.copyProperties(q, vo);
            result.add(vo);
        }

        if (result.isEmpty()) {
            throw new BusinessException(ResultCode.AI_PARSE_ERROR, "AI 未返回有效题目，请调整出题要求后重试");
        }
        return result;
    }

    /** 调用大模型并解析出 questions 数组，解析失败按 max-retry 重试 */
    private JSONArray callAndParse(String userPrompt) {
        int maxRetry = Math.max(1, aiProperties.getMaxRetry());
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            String content;
            try {
                content = aiClient.chat(SYSTEM_PROMPT, userPrompt);
            } catch (BusinessException e) {
                // API Key 缺失等确定性错误无需重试
                throw e;
            } catch (RuntimeException e) {
                last = e;
                log.warn("第 {} 次调用 AI 失败: {}", attempt, e.getMessage());
                continue;
            }
            try {
                JSONArray arr = parseQuestions(content);
                if (arr != null && !arr.isEmpty()) {
                    return arr;
                }
                last = new BusinessException(ResultCode.AI_PARSE_ERROR, "AI 返回中未找到题目数组");
            } catch (RuntimeException e) {
                last = e;
                log.warn("第 {} 次解析 AI 返回失败: {}", attempt, e.getMessage());
            }
        }
        throw last != null ? last : new BusinessException(ResultCode.AI_PARSE_ERROR, "AI 出题失败");
    }

    /** 从可能夹带多余文本的返回中提取 JSON 并取出 questions 数组 */
    private JSONArray parseQuestions(String content) {
        JSONObject root = AiJsonUtil.extractJsonObject(content);
        return root.getJSONArray("questions");
    }

    /** options 统一存为 JSON 字符串；非选择题或空值返回 null */
    private String normalizeOptions(Object options) {
        if (options == null) {
            return null;
        }
        if (options instanceof JSONArray arr) {
            return arr.isEmpty() ? null : arr.toString();
        }
        String str = options.toString().trim();
        return StrUtil.isBlank(str) || "null".equalsIgnoreCase(str) ? null : str;
    }

    private String buildUserPrompt(AiGenerateDTO dto, int difficulty) {
        String point = StrUtil.isBlank(dto.getKnowledgePoint()) ? "不限" : dto.getKnowledgePoint();
        return String.format("""
                请依据以下要求命题。
                科目：%s
                知识点：%s
                题型：%d（1=单选，2=多选，3=填空，4=简答）
                难度：%d（1=简单，2=中等，3=困难，本次为%s）
                题量：%d

                输出要求：
                1. 仅输出一个 JSON 对象，结构为 {"questions":[{...}]}，不要输出 ``` 代码块、注释或任何多余文本。
                2. 每个题目对象包含字段：
                   - title：题干（字符串）
                   - options：选项字符串数组；单选给 4 项、多选给 4-6 项；填空与简答置为 null
                   - answer：单选填正确项字母（如 "A"）；多选填字母组合（如 "ABD"）；填空多空用 "|" 分隔；简答填参考答案要点
                   - analysis：答案解析（字符串）
                3. 题目须紧扣科目与知识点，难度匹配，答案准确、唯一、无歧义。
                """,
                dto.getSubject(), point, dto.getType(), difficulty, diffText(difficulty), dto.getCount());
    }

    private String diffText(int diff) {
        return switch (diff) {
            case 1 -> "简单";
            case 2 -> "中等";
            case 3 -> "困难";
            default -> "中等";
        };
    }
}
