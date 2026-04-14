package com.opensearch.utility.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "scheduler")
public class SchedulerConfig {

    private HealthCheckConfig healthCheck = new HealthCheckConfig();

    @Data
    public static class HealthCheckConfig {
        private boolean enabled = true;
        private String cron = "0 */5 * * * *";
        private int timeoutSeconds = 30;
    }
}
