package com.blog.ai.config;

import com.alibaba.dashscope.utils.Constants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云百炼 DashScope 全局配置：API Key 等，供 TextEmbedding 等 SDK 使用。
 */
@Slf4j
@Configuration
public class DashScopeConfig {

    @Value("${app.dashscope.api-key:}")
    private String apiKey;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            Constants.apiKey = apiKey;
            log.info("DashScope API Key 已从配置注入");
        } else {
            log.warn("DashScope API Key 未配置，Embedding 等能力将不可用，请设置 app.dashscope.api-key 或环境变量 DASHSCOPE_API_KEY");
        }
    }
}
