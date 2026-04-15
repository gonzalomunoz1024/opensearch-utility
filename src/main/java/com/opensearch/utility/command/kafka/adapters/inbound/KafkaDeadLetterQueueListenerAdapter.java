package com.opensearch.utility.command.kafka.adapters.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearch.utility.command.kafka.domain.event.KafkaEventTransformFailedEvent;
import com.opensearch.utility.core.config.DlqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaDeadLetterQueueListenerAdapter {

    private final DlqConfig dlqConfig;
    private final ObjectMapper objectMapper;

    public KafkaDeadLetterQueueListenerAdapter(DlqConfig dlqConfig, ObjectMapper objectMapper) {
        this.dlqConfig = dlqConfig;
        this.objectMapper = objectMapper;
    }

    @EventListener(KafkaEventTransformFailedEvent.class)
    public void handleKafkaTransformFailure(KafkaEventTransformFailedEvent event) {
        if (!dlqConfig.isEnabled()) {
            log.debug("[KAFKA-DLQ] DLQ is disabled, skipping event: {}", event.getEventId());
            return;
        }

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n");
        logMessage.append("=".repeat(80)).append("\n");
        logMessage.append("KAFKA TRANSFORM DEAD LETTER EVENT\n");
        logMessage.append("=".repeat(80)).append("\n");
        logMessage.append("Event ID:        ").append(event.getEventId()).append("\n");
        logMessage.append("Timestamp:       ").append(event.getTimestamp()).append("\n");
        logMessage.append("Correlation ID:  ").append(event.getCorrelationId()).append("\n");
        logMessage.append("Source Topic:    ").append(event.getSourceTopic()).append("\n");
        logMessage.append("Failure Reason:  ").append(event.getFailureReason()).append("\n");
        logMessage.append("Exception Class: ").append(event.getExceptionClass()).append("\n");

        if (event.getKafkaPartition() != null) {
            logMessage.append("Kafka Partition: ").append(event.getKafkaPartition()).append("\n");
        }
        if (event.getKafkaOffset() != null) {
            logMessage.append("Kafka Offset:    ").append(event.getKafkaOffset()).append("\n");
        }

        if (dlqConfig.isIncludePayload() && event.getRawEvent() != null) {
            logMessage.append("-".repeat(80)).append("\n");
            logMessage.append("RAW EVENT PAYLOAD:\n");
            try {
                String payload = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(event.getRawEvent());
                if (payload.length() > dlqConfig.getMaxPayloadSize()) {
                    payload = payload.substring(0, dlqConfig.getMaxPayloadSize()) + "... [TRUNCATED]";
                }
                logMessage.append(payload).append("\n");
            } catch (JsonProcessingException e) {
                logMessage.append("Failed to serialize payload: ").append(e.getMessage()).append("\n");
            }
        }

        logMessage.append("=".repeat(80));

        switch (dlqConfig.getLogLevel().toUpperCase()) {
            case "ERROR" -> log.error(logMessage.toString());
            case "INFO" -> log.info(logMessage.toString());
            case "DEBUG" -> log.debug(logMessage.toString());
            default -> log.warn(logMessage.toString());
        }
    }
}
