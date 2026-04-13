package com.smartfilemanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.xunfei")
public class AIConfig {
    private String appId;
    private String apiSecret;
    private String apiKey;
    private String hostUrl = "https://spark-api.xf-yun.com/v3.5/chat";
    private String domain = "generalv3.5";
    private double temperature = 0.5;
    private int maxTokens = 4096;
}

