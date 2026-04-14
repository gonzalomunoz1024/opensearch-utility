package com.opensearch.utility.command.document.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opensearch.utility.command.document.domain.event.DocumentDeadLetterEvent;
import com.opensearch.utility.core.config.DlqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueueListener {

    private final DlqConfig dlqConfig;
    private final ObjectMapper objectMapper;

    public DeadLetterQueueListener(DlqConfig dlqConfig) {
        this.dlqConfig = dlqConfig;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @EventListener
    public void handleDeadLetterEvent(DocumentDeadLetterEvent event) {
        if (!dlqConfig.isEnabled()) {
            log.debug("DLQ is disabled, skipping event: {}", event.getEventId());
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
                String payload = objectMapper.writeValueAsString(event.getDocuments());
                if (payload.length() > dlqConfig.getMaxPayloadSize()) {
                    payload = payload.substring(0, dlqConfig.getMaxPayloadSize()) + "... [TRUNCATED]";
                }
                logMessage.append(payload).append("\n");
            } catch (JsonProcessingException e) {
                logMessage.append("Failed to serialize payload: ").append(e.getMessage()).append("\n");
            }
        }

        logMessage.append("=".repeat(80));

        // Log based on configured level
        switch (dlqConfig.getLogLevel().toUpperCase()) {
            case "ERROR":
                log.error(logMessage.toString());
                break;
            case "INFO":
                log.info(logMessage.toString());
                break;
            case "DEBUG":
                log.debug(logMessage.toString());
                break;
            default:
                log.warn(logMessage.toString());
        }
    }
}
