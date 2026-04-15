package com.opensearch.utility.core.config;

import com.opensearch.utility.command.kafka.config.KafkaTopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
@EnableConfigurationProperties({
        OpenSearchConfig.class,
        BatchConfig.class,
        RetryConfig.class,
        DlqConfig.class,
        KafkaTopicConfig.class
})
public class MainConfig {

    @Bean
    public SimpleApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        multicaster.setTaskExecutor(new SimpleAsyncTaskExecutor("event-"));
        return multicaster;
    }
}
