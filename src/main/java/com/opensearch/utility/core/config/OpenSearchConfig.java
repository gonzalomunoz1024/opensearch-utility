package com.opensearch.utility.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchConfig {

    private ClusterConfig cluster = new ClusterConfig();
    private SslConfig ssl = new SslConfig();

    @Data
    public static class ClusterConfig {
        private String url = "http://localhost:9200";
        private String username = "admin";
        private String password = "admin";
        private int connectionTimeoutMs = 5000;
        private int socketTimeoutMs = 60000;
        private int maxConnections = 100;
        private int maxConnectionsPerRoute = 20;
    }

    @Data
    public static class SslConfig {
        private boolean enabled = false;
        private boolean verifyHostname = true;
    }
}
