package com.opensearch.utility.command.document.adapters.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearch.utility.command.document.domain.event.DocumentDeadLetterEvent;
import com.opensearch.utility.core.config.DlqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterQueueListenerAdapter {

    private final DlqConfig dlqConfig;
    private final ObjectMapper objectMapper;

    public DeadLetterQueueListenerAdapter(DlqConfig dlqConfig, ObjectMapper objectMapper) {
        this.dlqConfig = dlqConfig;
        this.objectMapper = objectMapper;
    }

    @EventListener(DocumentDeadLetterEvent.class)
    public void handleDeadLetterEvent(DocumentDeadLetterEvent event) {
        if (!dlqConfig.isEnabled()) {
            log.debug("[DLQ] DLQ is disabled, skipping event: {}", event.getEventId());
            return;
        }

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n");
        logMessage.append("=".repeat(80)).append("\n");
        logMessage.append("DEAD LETTER QUEUE EVENT\n");
        logMessage.append("=".repeat(80)).append("\n");
        logMessage.append("Event ID:        ").append(event.getEventId()).append("\n");
        logMessage.append("Timestamp:       ").append(event.getTimestamp()).append("\n");
        logMessage.append("Correlation ID:  ").append(event.getCorrelationId()).append("\n");
        logMessage.append("Target Index:    ").append(event.getTargetIndex()).append("\n");
        logMessage.append("Source Endpoint: ").append(event.getSourceEndpoint()).append("\n");
        logMessage.append("Document Count:  ").append(event.getDocumentCount()).append("\n");
        logMessage.append("Document IDs:    ").append(event.getDocumentIds()).append("\n");
        logMessage.append("Failure Reason:  ").append(event.getFailureReason()).append("\n");
        logMessage.append("Original Retry Count: ").append(event.getOriginalReprocessCount()).append("\n");

        if (dlqConfig.isIncludePayload()) {
            logMessage.append("-".repeat(80)).append("\n");
            logMessage.append("PAYLOAD:\n");
            try {
                String payload = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(event.getDocuments());
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
