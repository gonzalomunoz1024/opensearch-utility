package com.opensearch.utility.command.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaTopicConfig {

    private boolean enabled = true;
    private String bootstrapServers = "localhost:9092";
    private String consumerGroup = "opensearch-utility";

    private Map<String, TopicSettings> topics = new HashMap<>();

    @Data
    public static class TopicSettings {
        private String name;
        private int concurrency = 1;
        private boolean enabled = true;
        private String dlqTopic;
    }
}
