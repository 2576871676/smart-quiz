package org.king.smartquiz.framework.config;

import org.king.smartquiz.framework.ai.AiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 供 AI 调用使用的 RestTemplate，连接/读取超时取自 ai.timeout。
 */
@Configuration
public class RestTemplateConfig {

    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate(AiProperties props) {
        int timeoutMillis = props.getTimeout() * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.min(timeoutMillis, 10_000));
        factory.setReadTimeout(timeoutMillis);
        return new RestTemplate(factory);
    }
}
