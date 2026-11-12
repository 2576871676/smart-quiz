package org.king.smartquiz.framework.ai;

/**
 * AI 对话客户端抽象。屏蔽不同大模型差异，业务层只依赖此接口。
 */
public interface AiClient {

    /**
     * 单轮对话。
     *
     * @param systemPrompt 系统提示词（角色设定）
     * @param userPrompt   用户提示词（具体任务）
     * @return 模型返回的文本内容
     */
    String chat(String systemPrompt, String userPrompt);
}
