package com.opensearch.utility.command.index.usecases;

import com.opensearch.utility.command.index.domain.Index;
import com.opensearch.utility.command.index.ports.inbound.IndexQueryPort;
import com.opensearch.utility.command.index.ports.outbound.IndexManagementPort;
import com.opensearch.utility.core.exception.IndexOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultIndexQueryUseCase implements IndexQueryUseCase, IndexQueryPort {

    private final IndexManagementPort indexManagementPort;

    @Override
    public Mono<Index> getIndex(String indexName) {
        log.debug("Getting index: {}", indexName);
        return indexManagementPort.indexExists(indexName)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IndexOperationException.IndexNotFoundException(indexName));
                    }
                    return indexManagementPort.getIndex(indexName);
                });
    }

    @Override
    public Flux<Index> listIndices() {
        log.debug("Listing all indices");
        return indexManagementPort.listIndices();
    }

    @Override
    public Mono<Boolean> indexExists(String indexName) {
        return indexManagementPort.indexExists(indexName);
    }
}
