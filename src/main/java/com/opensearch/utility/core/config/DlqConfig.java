package com.opensearch.utility.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dlq")
public class DlqConfig {

    private boolean enabled = true;
    private String logLevel = "WARN";
    private boolean includePayload = true;
    private int maxPayloadSize = 10000;
}
