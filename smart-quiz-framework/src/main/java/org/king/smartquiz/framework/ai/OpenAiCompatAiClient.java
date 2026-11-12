package org.king.smartquiz.framework.ai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.king.smartquiz.common.core.ResultCode;
import org.king.smartquiz.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议的真实大模型客户端，适用于通义千问 / 豆包 / DeepSeek 等。
 * 当 ai.mock=false（或未配置 ai.mock）时生效。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.mock", havingValue = "false", matchIfMissing = true)
public class OpenAiCompatAiClient implements AiClient {

    private final RestTemplate restTemplate;
    private final AiProperties props;

    public OpenAiCompatAiClient(RestTemplate aiRestTemplate, AiProperties props) {
        this.restTemplate = aiRestTemplate;
        this.props = props;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (StrUtil.isBlank(props.getApiKey()) || "your-api-key-here".equals(props.getApiKey())) {
            throw new BusinessException(ResultCode.AI_ERROR, "未配置有效的 AI API Key，请设置环境变量 AI_API_KEY 或开启 ai.mock=true");
        }
        String url = StrUtil.removeSuffix(props.getBaseUrl(), "/") + "/chat/completions";

        List<Map<String, String>> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("messages", messages);
        body.put("temperature", props.getTemperature());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey());

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            JSONObject json = JSONUtil.parseObj(resp.getBody());
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException(ResultCode.AI_ERROR, "AI 返回内容为空");
            }
            return choices.getJSONObject(0).getJSONObject("message").getStr("content");
        } catch (RestClientException e) {
            log.error("调用 AI 接口失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.AI_ERROR, "调用 AI 接口失败: " + e.getMessage());
        }
    }
}
