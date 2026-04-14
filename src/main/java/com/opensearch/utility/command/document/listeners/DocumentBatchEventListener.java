package com.opensearch.utility.command.document.listeners;

import com.opensearch.utility.command.document.domain.Document;
import com.opensearch.utility.command.document.domain.event.DocumentBatchFailedEvent;
import com.opensearch.utility.command.document.domain.event.DocumentBatchReceivedEvent;
import com.opensearch.utility.command.document.domain.event.DocumentBatchSavedEvent;
import com.opensearch.utility.command.document.domain.event.DocumentDeadLetterEvent;
import com.opensearch.utility.command.document.ports.outbound.DocumentPersistencePort;
import com.opensearch.utility.core.domain.RetryableEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentBatchEventListener {

    private final DocumentPersistencePort documentPersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    public void handleBatchReceived(DocumentBatchReceivedEvent event) {
        log.info("Processing batch event: {} documents for index {}, reprocessCount: {}",
                event.getBatchSize(), event.getTargetIndex(), event.getReprocessCount());

        long startTime = System.currentTimeMillis();

        documentPersistencePort.bulkSave(event.getTargetIndex(), event.getDocuments())
                .subscribe(
                        result -> {
                            long processingTime = System.currentTimeMillis() - startTime;

                            if (!result.hasFailures()) {
                                // All documents saved successfully
                                publishSuccessEvent(event, processingTime);
                            } else {
                                // Some documents failed
                                handlePartialFailure(event, result.getFailedDocumentIds(),
                                        buildErrorReason(result.getFailedItems()));
                            }
                        },
                        error -> {
                            log.error("Batch save failed completely: {}", error.getMessage());
                            handleCompleteFailure(event, error.getMessage());
                        }
                );
    }

    private void publishSuccessEvent(DocumentBatchReceivedEvent event, long processingTime) {
        List<String> documentIds = event.getDocuments().stream()
                .map(Document::getId)
                .toList();

        DocumentBatchSavedEvent successEvent = DocumentBatchSavedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .correlationId(event.getCorrelationId())
                .targetIndex(event.getTargetIndex())
                .documentCount(event.getBatchSize())
                .documentIds(documentIds)
                .processingTimeMs(processingTime)
                .build();

        eventPublisher.publishEvent(successEvent);
        log.info("Batch saved successfully: {} documents in {}ms",
                event.getBatchSize(), processingTime);
    }

    private void handlePartialFailure(DocumentBatchReceivedEvent event,
                                       List<String> failedDocumentIds,
                                       String errorReason) {
        log.warn("Partial batch failure: {}/{} documents failed",
                failedDocumentIds.size(), event.getBatchSize());

        // Extract failed documents
        List<Document> failedDocuments = event.getDocuments().stream()
                .filter(doc -> failedDocumentIds.contains(doc.getId()))
                .toList();

        if (event.canRetry()) {
            // Retry with decremented count
            retryFailedDocuments(event, failedDocuments, errorReason);
        } else {
            // Send to DLQ
            sendToDeadLetterQueue(event, failedDocuments, errorReason);
        }

        // Publish partial failure event
        publishFailedEvent(event, failedDocumentIds, errorReason);
    }

    private void handleCompleteFailure(DocumentBatchReceivedEvent event, String errorReason) {
        if (event.canRetry()) {
            retryFailedDocuments(event, event.getDocuments(), errorReason);
        } else {
            sendToDeadLetterQueue(event, event.getDocuments(), errorReason);
        }

        List<String> docIds = event.getDocuments().stream()
                .map(Document::getId)
                .toList();
        publishFailedEvent(event, docIds, errorReason);
    }

    private void retryFailedDocuments(DocumentBatchReceivedEvent originalEvent,
                                       List<Document> failedDocuments,
                                       String errorReason) {
        int newReprocessCount = originalEvent.getReprocessCount() - 1;

        log.info("Retrying {} failed documents, remaining attempts: {}",
                failedDocuments.size(), newReprocessCount);

        DocumentBatchReceivedEvent retryEvent = originalEvent.toBuilder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .documents(failedDocuments)
                .reprocessCount(newReprocessCount)
                .build();

        eventPublisher.publishEvent(retryEvent);
    }

    private void sendToDeadLetterQueue(DocumentBatchReceivedEvent originalEvent,
                                        List<Document> failedDocuments,
                                        String errorReason) {
        log.warn("Sending {} documents to DLQ - retry count exhausted", failedDocuments.size());

        DocumentDeadLetterEvent dlqEvent = DocumentDeadLetterEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .correlationId(originalEvent.getCorrelationId())
                .targetIndex(originalEvent.getTargetIndex())
                .documents(failedDocuments)
                .sourceEndpoint(originalEvent.getSourceEndpoint())
                .failureReason(errorReason)
                .originalReprocessCount(RetryableEvent.DEFAULT_REPROCESS_COUNT)
                .build();

        eventPublisher.publishEvent(dlqEvent);
    }

    private void publishFailedEvent(DocumentBatchReceivedEvent event,
                                     List<String> failedDocumentIds,
                                     String errorReason) {
        DocumentBatchFailedEvent failedEvent = DocumentBatchFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .correlationId(event.getCorrelationId())
                .targetIndex(event.getTargetIndex())
                .failedDocumentIds(failedDocumentIds)
                .failureReason(errorReason)
                .remainingRetries(event.getReprocessCount())
                .build();

        eventPublisher.publishEvent(failedEvent);
    }

    private String buildErrorReason(List<com.opensearch.utility.command.document.domain.BulkOperationResult.BulkItemResult> failedItems) {
        if (failedItems.isEmpty()) return "Unknown error";

        return failedItems.stream()
                .map(item -> item.getErrorType() + ": " + item.getErrorReason())
                .distinct()
                .reduce((a, b) -> a + "; " + b)
                .orElse("Unknown error");
    }
}
