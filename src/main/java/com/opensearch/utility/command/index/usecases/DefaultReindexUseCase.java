package com.opensearch.utility.command.index.usecases;

import com.opensearch.utility.command.index.domain.command.ReindexCommand;
import com.opensearch.utility.command.index.domain.dto.outbound.ReindexResponse;
import com.opensearch.utility.command.index.ports.outbound.IndexManagementPort;
import com.opensearch.utility.core.exception.IndexOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultReindexUseCase implements ReindexUseCase {

    private final IndexManagementPort indexManagementPort;

    @Override
    public Mono<ReindexResponse> execute(ReindexCommand command) {
        log.info("Starting reindex from {} to {}", command.getSourceIndex(), command.getDestinationIndex());

        return validateIndices(command)
                .then(indexManagementPort.reindex(command))
                .doOnSuccess(response -> log.info("Reindex completed: {} documents migrated from {} to {}",
                        response.getTotal(), command.getSourceIndex(), command.getDestinationIndex()))
                .doOnError(error -> log.error("Reindex failed: {}", error.getMessage()));
    }

    private Mono<Void> validateIndices(ReindexCommand command) {
        return indexManagementPort.indexExists(command.getSourceIndex())
                .flatMap(sourceExists -> {
                    if (!sourceExists) {
                        return Mono.error(new IndexOperationException.IndexNotFoundException(command.getSourceIndex()));
                    }
                    return indexManagementPort.indexExists(command.getDestinationIndex());
                })
                .flatMap(destExists -> {
                    if (!destExists) {
                        return Mono.error(new IndexOperationException.IndexNotFoundException(command.getDestinationIndex()));
                    }
                    return Mono.empty();
                });
    }
}
