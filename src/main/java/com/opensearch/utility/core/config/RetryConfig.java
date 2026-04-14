package com.opensearch.utility.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "retry")
public class RetryConfig {

    private int maxAttempts = 3;
    private int backoffMultiplier = 2;
    private long initialDelayMs = 1000;
    private long maxDelayMs = 30000;
}
