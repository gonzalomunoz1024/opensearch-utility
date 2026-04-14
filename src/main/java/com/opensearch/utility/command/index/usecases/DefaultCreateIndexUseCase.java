package com.opensearch.utility.command.index.usecases;

import com.opensearch.utility.command.index.domain.Index;
import com.opensearch.utility.command.index.domain.command.CreateIndexCommand;
import com.opensearch.utility.command.index.domain.event.IndexCreatedEvent;
import com.opensearch.utility.command.index.ports.inbound.CreateIndexPort;
import com.opensearch.utility.command.index.ports.outbound.IndexManagementPort;
import com.opensearch.utility.core.exception.IndexOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCreateIndexUseCase implements CreateIndexUseCase, CreateIndexPort {

    private final IndexManagementPort indexManagementPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Mono<Index> execute(CreateIndexCommand command) {
        return createIndex(command);
    }

    @Override
    public Mono<Index> createIndex(CreateIndexCommand command) {
        log.info("Creating index: {}", command.getIndexName());

        return indexManagementPort.indexExists(command.getIndexName())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IndexOperationException.IndexAlreadyExistsException(command.getIndexName()));
                    }
                    return indexManagementPort.createIndex(
                            command.getIndexName(),
                            command.getSettings(),
                            command.getMappings()
                    );
                })
                .doOnSuccess(index -> {
                    IndexCreatedEvent event = IndexCreatedEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .timestamp(Instant.now())
                            .correlationId(UUID.randomUUID().toString())
                            .index(index)
                            .build();
                    eventPublisher.publishEvent(event);
                    log.info("Published IndexCreatedEvent for index: {}", index.getName());
                });
    }
}
