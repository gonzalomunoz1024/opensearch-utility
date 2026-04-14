package com.opensearch.utility.command.document.usecases;

import com.opensearch.utility.command.document.domain.Document;
import com.opensearch.utility.command.document.domain.command.PopulateIndexCommand;
import com.opensearch.utility.command.document.domain.dto.outbound.PopulateStatusResponse;
import com.opensearch.utility.command.document.domain.event.DocumentBatchReceivedEvent;
import com.opensearch.utility.command.document.ports.inbound.PopulateIndexPort;
import com.opensearch.utility.command.document.ports.outbound.CustomerDataSourcePort;
import com.opensearch.utility.command.index.ports.outbound.IndexManagementPort;
import com.opensearch.utility.core.config.BatchConfig;
import com.opensearch.utility.core.domain.RetryableEvent;
import com.opensearch.utility.core.exception.IndexOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultPopulateIndexUseCase implements PopulateIndexUseCase, PopulateIndexPort {

    private final CustomerDataSourcePort customerDataSourcePort;
    private final IndexManagementPort indexManagementPort;
    private final ApplicationEventPublisher eventPublisher;
    private final BatchConfig batchConfig;

    @Override
    public Mono<PopulateStatusResponse> execute(PopulateIndexCommand command) {
        return populateIndex(command);
    }

    @Override
    public Mono<PopulateStatusResponse> populateIndex(PopulateIndexCommand command) {
        String jobId = UUID.randomUUID().toString();
        String correlationId = command.getCorrelationId() != null
                ? command.getCorrelationId()
                : UUID.randomUUID().toString();

        log.info("Starting populate job {} for index {} from {}",
                jobId, command.getTargetIndex(), command.getSourceEndpoint());

        Instant startedAt = Instant.now();
        AtomicLong documentsProcessed = new AtomicLong(0);

        // Verify index exists
        return indexManagementPort.indexExists(command.getTargetIndex())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IndexOperationException.IndexNotFoundException(command.getTargetIndex()));
                    }

                    // Fetch documents and batch them using bufferTimeout
                    // 20 items OR 5 seconds - whichever comes first
                    return customerDataSourcePort.fetchDocuments(command.getSourceEndpoint())
                            .bufferTimeout(
                                    batchConfig.getSize(),
                                    Duration.ofSeconds(batchConfig.getTimeoutSeconds())
                            )
                            .doOnNext(batch -> {
                                documentsProcessed.addAndGet(batch.size());
                                publishBatchEvent(command.getTargetIndex(), batch,
                                        command.getSourceEndpoint(), correlationId);
                            })
                            .then(Mono.defer(() -> {
                                log.info("Populate job {} completed. Total documents queued: {}",
                                        jobId, documentsProcessed.get());

                                return Mono.just(PopulateStatusResponse.builder()
                                        .jobId(jobId)
                                        .targetIndex(command.getTargetIndex())
                                        .sourceEndpoint(command.getSourceEndpoint())
                                        .status("PROCESSING")
                                        .documentsProcessed(documentsProcessed.get())
                                        .startedAt(startedAt)
                                        .build());
                            }));
                });
    }

    private void publishBatchEvent(String targetIndex, List<Document> documents,
                                    String sourceEndpoint, String correlationId) {
        DocumentBatchReceivedEvent event = DocumentBatchReceivedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .targetIndex(targetIndex)
                .documents(documents)
                .sourceEndpoint(sourceEndpoint)
                .reprocessCount(RetryableEvent.DEFAULT_REPROCESS_COUNT)
                .build();

        eventPublisher.publishEvent(event);
        log.debug("Published batch event with {} documents for index {}",
                documents.size(), targetIndex);
    }
}
