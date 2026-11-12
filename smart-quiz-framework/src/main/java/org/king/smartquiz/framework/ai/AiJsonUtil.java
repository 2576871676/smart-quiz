package org.king.smartquiz.framework.ai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.king.smartquiz.common.core.ResultCode;
import org.king.smartquiz.common.exception.BusinessException;

/**
 * AI 返回内容的 JSON 容错提取工具。
 * 大模型常在 JSON 外夹带 ```代码块围栏或解释文字，此处统一剥离。
 */
public final class AiJsonUtil {

    private AiJsonUtil() {
    }

    /** 从原始返回中提取出纯 JSON 字符串（首个 '{' 到末个 '}'） */
    public static String extractJsonString(String raw) {
        if (StrUtil.isBlank(raw)) {
            throw new BusinessException(ResultCode.AI_PARSE_ERROR, "AI 返回内容为空");
        }
        String s = raw.trim();
        int fence = s.indexOf("```");
        if (fence >= 0) {
            s = s.substring(fence + 3);
            if (s.startsWith("json")) {
                s = s.substring(4);
            }
            int endFence = s.indexOf("```");
            if (endFence >= 0) {
                s = s.substring(0, endFence);
            }
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(ResultCode.AI_PARSE_ERROR, "AI 返回中未找到 JSON 对象");
        }
        return s.substring(start, end + 1);
    }

    /** 提取并解析为 JSONObject */
    public static JSONObject extractJsonObject(String raw) {
        return JSONUtil.parseObj(extractJsonString(raw));
    }
}
