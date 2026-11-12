package org.king.smartquiz.framework.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 大模型配置，对应 application.yml 的 ai.* 节点。
 * 采用 OpenAI 兼容协议，切换豆包/千问/DeepSeek 只需改 base-url 与 model。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 是否启用 Mock 模式（无 API Key 时离线跑通全流程） */
    private boolean mock = true;

    /** API Key，建议通过环境变量 AI_API_KEY 注入 */
    private String apiKey;

    /** OpenAI 兼容服务地址，如 https://dashscope.aliyuncs.com/compatible-mode/v1 */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 模型名，如 qwen-plus */
    private String model = "qwen-plus";

    /** 单次调用超时（秒） */
    private int timeout = 60;

    /** 解析失败时的最大重试次数 */
    private int maxRetry = 3;

    /** 采样温度 */
    private double temperature = 0.8;
}
