package com.opensearch.utility.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "batch")
public class BatchConfig {

    private int size = 20;
    private int timeoutSeconds = 5;
    private int maxConcurrentBatches = 10;
    private int bufferCapacity = 1000;
}
