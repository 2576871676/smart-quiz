package org.king.smartquiz.system.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.king.smartquiz.common.exception.BusinessException;
import org.king.smartquiz.framework.ai.AiClient;
import org.king.smartquiz.framework.ai.AiJsonUtil;
import org.king.smartquiz.framework.ai.AiProperties;
import org.king.smartquiz.system.entity.Question;
import org.springframework.stereotype.Component;

/**
 * 统一判分组件：练习模式与试卷模式共用，保证两种模式判分口径一致。
 * 客观题（单选/多选/填空）本地即时判分；主观题（简答）调用大模型评分，含容错解析、重试与降级。
 */
@Slf4j
@Component
public class QuestionGrader {

    private static final String GRADE_SYSTEM =
            "你是一名严格而公正的阅卷老师，依据参考答案对学生作答评分。只输出 JSON，不输出任何多余文本。";
    /** 主观题判为「正确」的得分率阈值 */
    private static final double PASS_RATE = 0.6;

    private final AiClient aiClient;
    private final AiProperties aiProperties;

    public QuestionGrader(AiClient aiClient, AiProperties aiProperties) {
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
    }

    /** 题目满分（缺省 5 分） */
    public int fullScore(Question q) {
        return q.getScore() == null ? 5 : q.getScore();
    }

    /**
     * 对单题作答判分（满分取题目自身分值）。
     *
     * @return 判分结果（得分、是否正确、AI 评语、满分）
     */
    public GradeOutcome grade(Question q, String userAnswer) {
        return grade(q, userAnswer, null);
    }

    /**
     * 对单题作答判分，允许覆盖满分（试卷模式下每题分值由试卷指定）。
     *
     * @param fullOverride 覆盖满分；为 null 时取题目自身分值
     * @return 判分结果（得分、是否正确、AI 评语、满分）
     */
    public GradeOutcome grade(Question q, String userAnswer, Integer fullOverride) {
        int full = fullOverride != null ? fullOverride : fullScore(q);
        int type = q.getType() == null ? 0 : q.getType();
        if (type == 4) {
            GradeResult gr = aiGrade(q, userAnswer, full);
            return new GradeOutcome(gr.score(), gr.isCorrect(), gr.comment(), full);
        }
        boolean ok = judgeObjective(type, q.getAnswer(), userAnswer);
        return new GradeOutcome(ok ? full : 0, ok ? 1 : 0, null, full);
    }

    // ==================== 客观题判分 ====================

    private boolean judgeObjective(int type, String reference, String userAnswer) {
        if (StrUtil.isBlank(userAnswer) || StrUtil.isBlank(reference)) {
            return false;
        }
        String ua = userAnswer.trim();
        String ref = reference.trim();
        return switch (type) {
            case 1 -> ua.equalsIgnoreCase(ref);                 // 单选
            case 2 -> normalizeMulti(ua).equals(normalizeMulti(ref)); // 多选：全对才得分
            case 3 -> judgeFill(ref, ua);                          // 填空：各空关键词匹配
            default -> ua.equalsIgnoreCase(ref);
        };
    }

    /** 多选题答案归一：仅保留字母、大写、去重、排序 */
    private String normalizeMulti(String s) {
        return s.toUpperCase().chars()
                .filter(Character::isLetter)
                .distinct()
                .sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /** 填空题：参考答案以 | 分隔多个空，学生答案需包含每个空的关键词（忽略大小写与空白） */
    private boolean judgeFill(String reference, String userAnswer) {
        String ua = userAnswer.replaceAll("\\s", "").toLowerCase();
        for (String blank : reference.split("\\|")) {
            String seg = blank.replaceAll("\\s", "").toLowerCase();
            if (seg.isEmpty()) {
                continue;
            }
            if (!ua.contains(seg)) {
                return false;
            }
        }
        return true;
    }

    // ==================== 主观题 AI 批改 ====================

    private GradeResult aiGrade(Question q, String userAnswer, int full) {
        if (StrUtil.isBlank(userAnswer)) {
            return new GradeResult(0, 0, "未作答，得 0 分。");
        }
        String prompt = buildGradePrompt(q, userAnswer, full);
        int maxRetry = Math.max(1, aiProperties.getMaxRetry());
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                String content = aiClient.chat(GRADE_SYSTEM, prompt);
                JSONObject obj = AiJsonUtil.extractJsonObject(content);
                Integer raw = obj.getInt("score");
                int score = raw == null ? 0 : Math.max(0, Math.min(full, raw));
                int passLine = (int) Math.ceil(full * PASS_RATE);
                int isCorrect = score >= passLine ? 1 : 0;
                return new GradeResult(score, isCorrect, obj.getStr("comment"));
            } catch (BusinessException e) {
                log.warn("AI 批改失败（确定性错误，不重试）: {}", e.getMessage());
                break;
            } catch (RuntimeException e) {
                log.warn("第 {} 次 AI 批改失败: {}", attempt, e.getMessage());
            }
        }
        // 降级：标记未判分，待人工复核
        return new GradeResult(0, null, "AI 批改失败，待人工复核");
    }

    private String buildGradePrompt(Question q, String userAnswer, int full) {
        return String.format("""
                请批改以下主观题并给出得分与评语。
                题目：%s
                满分：%d分
                参考答案：%s
                学生答案：%s
                评分要求：仅输出 JSON 对象 {"score": 0到%d的整数, "comment": "评分理由与改进建议(80字内)"}；不要输出 ``` 代码块或多余文本。意思正确即应给分，不因表述与参考答案不同而扣分。
                """, q.getTitle(), full, StrUtil.nullToDefault(q.getAnswer(), "（无）"), userAnswer, full);
    }

    /** 判分结果载体（对外） */
    public record GradeOutcome(int score, Integer isCorrect, String comment, int fullScore) {
    }

    /** AI 批改内部结果载体 */
    private record GradeResult(int score, Integer isCorrect, String comment) {
    }
}
