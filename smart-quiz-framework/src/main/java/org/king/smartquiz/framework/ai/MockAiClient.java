package org.king.smartquiz.framework.ai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock AI 客户端：无 API Key 时离线跑通「生成 → 解析 → 入库」全流程，
 * 也便于毕设答辩时脱网演示。根据提示词中的科目/题型/难度/题量动态产出样例题。
 * 当 ai.mock=true 时生效。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.mock", havingValue = "true")
public class MockAiClient implements AiClient {

    private static final Pattern SUBJECT = Pattern.compile("科目：\\s*(.+)");
    private static final Pattern POINT = Pattern.compile("知识点：\\s*(.+)");
    private static final Pattern TYPE = Pattern.compile("题型：\\s*(\\d+)");
    private static final Pattern DIFF = Pattern.compile("难度：\\s*(\\d+)");
    private static final Pattern COUNT = Pattern.compile("题量：\\s*(\\d+)");
    private static final Pattern FULL_SCORE = Pattern.compile("满分：\\s*(\\d+)");
    private static final Pattern STUDENT_ANSWER = Pattern.compile("学生答案：\\s*(.*)");
    private static final Pattern ACCURACY = Pattern.compile("总正确率：\\s*([0-9.]+)");

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        // 主观题批改场景：提示词含「学生答案」时返回评分 JSON
        if (userPrompt != null && userPrompt.contains("学生答案")) {
            return mockGrade(userPrompt);
        }
        // 学习建议场景：提示词含「学习建议」时返回纯文本建议
        if (userPrompt != null && userPrompt.contains("学习建议")) {
            return mockAdvice(userPrompt);
        }
        return mockGenerate(userPrompt);
    }

    /** Mock 学习建议：依据总正确率给出分层的纯文本建议，无 JSON、无代码块 */
    private String mockAdvice(String userPrompt) {
        double accuracy = Double.parseDouble(find(ACCURACY, userPrompt, "0"));
        String level = accuracy >= 85 ? "整体表现优秀" : accuracy >= 60 ? "基础尚可、仍有提升空间" : "基础较薄弱、需要重点补强";
        log.info("[MockAI] 生成学习建议，总正确率={}，评价={}", accuracy, level);
        return String.format("""
                学习建议（Mock 模式生成，接入真实大模型后将由 AI 产出）：
                1. 总体评价：%s（当前总正确率约 %s%%）。
                2. 优先攻克「薄弱知识点TOP5」中排名靠前的知识点，先回归教材梳理概念，再做专项练习。
                3. 每天固定处理错题本中未掌握的题目，重做并对照解析，直到能独立答对为止。
                4. 保持答题节奏，建议每天完成 10-20 题，兼顾正确率与覆盖面，避免只刷简单题。
                5. 对主观题按参考答案要点组织作答，训练结构化表达，减少失分。""", level, String.valueOf(accuracy));
    }

    /** Mock 主观题批改：按满分 80% 给分，未作答给 0 分 */
    private String mockGrade(String userPrompt) {
        int full = Integer.parseInt(find(FULL_SCORE, userPrompt, "10"));
        Matcher m = STUDENT_ANSWER.matcher(userPrompt);
        String answer = m.find() ? m.group(1).trim() : "";
        JSONObject result = new JSONObject();
        if (StrUtil.isBlank(answer)) {
            result.set("score", 0);
            result.set("comment", "未作答，得 0 分。");
        } else {
            int score = Math.max(1, (int) Math.round(full * 0.8));
            result.set("score", score);
            result.set("comment", "Mock 批改：覆盖了主要要点（" + score + "/" + full + "），表述可更严谨完整。");
        }
        log.info("[MockAI] 主观题批改，满分={}，判分={}", full, result.get("score"));
        return result.toString();
    }

    /** Mock 出题：按提示词中的科目/题型/难度/题量生成样例题目 */
    private String mockGenerate(String userPrompt) {
        String subject = find(SUBJECT, userPrompt, "综合");
        String point = find(POINT, userPrompt, "基础知识");
        int type = Integer.parseInt(find(TYPE, userPrompt, "1"));
        String difficulty = diffText(Integer.parseInt(find(DIFF, userPrompt, "2")));
        int count = Integer.parseInt(find(COUNT, userPrompt, "3"));
        if (count < 1) {
            count = 1;
        }
        if (count > 10) {
            count = 10;
        }
        log.info("[MockAI] 生成 {} 道《{}》题目，知识点={}，题型={}，难度={}", count, subject, point, type, difficulty);

        JSONArray questions = new JSONArray();
        for (int i = 1; i <= count; i++) {
            JSONObject q = new JSONObject();
            q.set("title", String.format("【%s·%s】%s题示例%d：请说明%s中「%s」的关键要点。",
                    subject, point, typeText(type), i, subject, point));
            if (type == 1 || type == 2) {
                JSONArray options = new JSONArray();
                options.set("A. " + point + "选项一（" + difficulty + "）");
                options.set("B. " + point + "选项二");
                options.set("C. " + point + "选项三");
                options.set("D. " + point + "选项四");
                q.set("options", options);
                q.set("answer", type == 1 ? "A" : "AB");
            } else {
                q.set("options", null);
                q.set("answer", type == 3 ? point + "答案一|" + point + "答案二" : "应覆盖" + point + "的定义、原理与典型应用三点。");
            }
            q.set("analysis", "这是 Mock 模式生成的样例解析，接入真实大模型后将被 AI 返回内容替换。");
            questions.set(q);
        }

        JSONObject root = new JSONObject();
        root.set("questions", questions);
        return JSONUtil.toJsonStr(root);
    }

    private String find(Pattern p, String text, String def) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : def;
    }

    private String typeText(int type) {
        return switch (type) {
            case 1 -> "单选";
            case 2 -> "多选";
            case 3 -> "填空";
            case 4 -> "简答";
            default -> "未知";
        };
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
