package com.opensearch.utility.command.document.adapters.inbound;

import com.opensearch.utility.command.document.domain.BulkOperationResult;
import com.opensearch.utility.command.document.domain.Document;
import com.opensearch.utility.command.document.domain.event.DocumentBatchFailedEvent;
import com.opensearch.utility.command.document.domain.event.DocumentBatchReceivedEvent;
import com.opensearch.utility.command.document.domain.event.DocumentBatchSavedEvent;
import com.opensearch.utility.command.document.domain.event.DocumentDeadLetterEvent;
import com.opensearch.utility.command.document.ports.inbound.DocumentBatchEventListenerPort;
import com.opensearch.utility.command.document.ports.outbound.DocumentPersistencePort;
import com.opensearch.utility.core.config.BatchConfig;
import com.opensearch.utility.core.domain.RetryableEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;

@Slf4j
@Component
public class DocumentBatchEventListenerAdapter implements DocumentBatchEventListenerPort<DocumentBatchReceivedEvent> {

    public final Sinks.EmitFailureHandler emitFailureHandler =
            (signalType, emitResult) -> emitResult.equals(Sinks.EmitResult.FAIL_NON_SERIALIZED);

    private final DocumentPersistencePort documentPersistencePort;
    private final ApplicationEventPublisher eventPublisher;
    private final BatchConfig batchConfig;

    private Sinks.Many<DocumentBatchReceivedEvent> aggregateSink;
    private Disposable sinkSubscription;

    public DocumentBatchEventListenerAdapter(
            DocumentPersistencePort documentPersistencePort,
            ApplicationEventPublisher eventPublisher,
            BatchConfig batchConfig) {
        this.documentPersistencePort = documentPersistencePort;
        this.eventPublisher = eventPublisher;
        this.batchConfig = batchConfig;
    }

    @PostConstruct
    void init() {
        aggregateSink = Sinks.many()
                .unicast()
                .onBackpressureBuffer(new ArrayDeque<>(4096));

        sinkSubscription = aggregateSink
                .asFlux()
                .publishOn(Schedulers.boundedElastic(), batchConfig.getMaxConcurrentBatches())
                .concatMap(this::processBatch)
                .doOnCancel(() -> log.warn("[BATCH] Document batch stream cancelled"))
                .doOnComplete(() -> log.info("[BATCH] Document batch stream completed"))
                .subscribe();

        log.info("[BATCH] Document batch event listener initialized with buffer size 4096");
    }

    @PreDestroy
    void cleanup() {
        if (sinkSubscription != null && !sinkSubscription.isDisposed()) {
            sinkSubscription.dispose();
            log.info("[BATCH] Document batch event listener disposed");
        }
    }

    @Override
    @EventListener(DocumentBatchReceivedEvent.class)
    public void processEvent(DocumentBatchReceivedEvent event) {
        log.debug("[BATCH] Event received: {} documents for index {}, reprocessCount: {}",
                event.getBatchSize(), event.getTargetIndex(), event.getReprocessCount());
        this.aggregateSink.emitNext(event, emitFailureHandler);
    }

    private Mono<BulkOperationResult> processBatch(DocumentBatchReceivedEvent event) {
        log.info("[BATCH] Processing batch: {} documents for index {}, reprocessCount: {}",
                event.getBatchSize(), event.getTargetIndex(), event.getReprocessCount());

        long startTime = System.currentTimeMillis();

        return documentPersistencePort.bulkSave(event.getTargetIndex(), event.getDocuments())
                .doOnSuccess(result -> {
                    long processingTime = System.currentTimeMillis() - startTime;
                    handleResult(event, result, processingTime);
                })
                .onErrorResume(error -> {
                    log.error("[BATCH] Batch save failed completely: {}", error.getMessage());
                    handleCompleteFailure(event, error.getMessage());
                    return Mono.empty();
                });
    }

    private void handleResult(DocumentBatchReceivedEvent event, BulkOperationResult result, long processingTime) {
        if (!result.hasFailures()) {
            publishSuccessEvent(event, processingTime);
        } else {
            List<String> failedDocumentIds = result.getFailedDocumentIds();
            String errorReason = buildErrorReason(result.getFailedItems());
            handlePartialFailure(event, failedDocumentIds, errorReason);
        }
    }

    private void publishSuccessEvent(DocumentBatchReceivedEvent event, long processingTime) {
        List<String> documentIds = event.getDocuments().stream()
                .map(Document::getId)
                .toList();

        DocumentBatchSavedEvent successEvent = DocumentBatchSavedEvent.builder()
                .withDefaults()
                .correlationId(event.getCorrelationId())
                .targetIndex(event.getTargetIndex())
                .documentCount(event.getBatchSize())
                .documentIds(documentIds)
                .processingTimeMs(processingTime)
                .build();

        eventPublisher.publishEvent(successEvent);
        log.info("[BATCH] Batch saved successfully: {} documents in {}ms",
                event.getBatchSize(), processingTime);
    }

    private void handlePartialFailure(DocumentBatchReceivedEvent event,
                                       List<String> failedDocumentIds,
                                       String errorReason) {
        log.warn("[BATCH] Partial batch failure: {}/{} documents failed",
                failedDocumentIds.size(), event.getBatchSize());

        List<Document> failedDocuments = event.getDocuments().stream()
                .filter(doc -> failedDocumentIds.contains(doc.getId()))
                .toList();

        if (event.canRetry()) {
            retryFailedDocuments(event, failedDocuments, errorReason);
        } else {
            sendToDeadLetterQueue(event, failedDocuments, errorReason);
        }

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

        log.info("[BATCH] Retrying {} failed documents, remaining attempts: {}",
                failedDocuments.size(), newReprocessCount);

        DocumentBatchReceivedEvent retryEvent = originalEvent.toBuilder()
                .withDefaults()
                .correlationId(originalEvent.getCorrelationId())
                .documents(failedDocuments)
                .reprocessCount(newReprocessCount)
                .build();

        eventPublisher.publishEvent(retryEvent);
    }

    private void sendToDeadLetterQueue(DocumentBatchReceivedEvent originalEvent,
                                        List<Document> failedDocuments,
                                        String errorReason) {
        log.warn("[BATCH] Sending {} documents to DLQ - retry count exhausted", failedDocuments.size());

        DocumentDeadLetterEvent dlqEvent = DocumentDeadLetterEvent.builder()
                .withDefaults()
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
                .withDefaults()
                .correlationId(event.getCorrelationId())
                .targetIndex(event.getTargetIndex())
                .failedDocumentIds(failedDocumentIds)
                .failureReason(errorReason)
                .remainingRetries(event.getReprocessCount())
                .build();

        eventPublisher.publishEvent(failedEvent);
    }

    private String buildErrorReason(List<BulkOperationResult.BulkItemResult> failedItems) {
        if (failedItems.isEmpty()) return "Unknown error";

        return failedItems.stream()
                .map(item -> item.getErrorType() + ": " + item.getErrorReason())
                .distinct()
                .reduce((a, b) -> a + "; " + b)
                .orElse("Unknown error");
    }
}
