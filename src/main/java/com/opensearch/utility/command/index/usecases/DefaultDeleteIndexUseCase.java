package com.opensearch.utility.command.index.usecases;

import com.opensearch.utility.command.index.domain.command.DeleteIndexCommand;
import com.opensearch.utility.command.index.domain.event.IndexDeletedEvent;
import com.opensearch.utility.command.index.ports.inbound.DeleteIndexPort;
import com.opensearch.utility.command.index.ports.outbound.IndexManagementPort;
import com.opensearch.utility.core.exception.IndexOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class DefaultDeleteIndexUseCase implements DeleteIndexUseCase, DeleteIndexPort {

    private final IndexManagementPort indexManagementPort;
    private final ApplicationEventPublisher eventPublisher;

    public DefaultDeleteIndexUseCase(IndexManagementPort indexManagementPort,
                                      ApplicationEventPublisher eventPublisher) {
        this.indexManagementPort = indexManagementPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<Void> execute(DeleteIndexCommand command) {
        return deleteIndex(command);
    }

    @Override
    public Mono<Void> deleteIndex(DeleteIndexCommand command) {
        log.info("Deleting index: {}", command.getIndexName());

        return indexManagementPort.indexExists(command.getIndexName())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IndexOperationException.IndexNotFoundException(command.getIndexName()));
                    }
                    return indexManagementPort.deleteIndex(command.getIndexName());
                })
                .doOnSuccess(v -> {
                    IndexDeletedEvent event = IndexDeletedEvent.builder()
                            .withDefaults()
                            .indexName(command.getIndexName())
                            .build();
                    eventPublisher.publishEvent(event);
                    log.info("Published IndexDeletedEvent for index: {}", command.getIndexName());
                });
    }
}
