package com.opensearch.utility.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        OpenSearchConfig.class,
        BatchConfig.class,
        RetryConfig.class,
        DlqConfig.class
})
public class MainConfig {
    // Central configuration class
    // Additional registry beans can be added here as needed
}
