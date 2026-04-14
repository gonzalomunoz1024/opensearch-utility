package com.opensearch.utility.command.index.usecases;

import com.opensearch.utility.command.index.domain.Index;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IndexQueryUseCase {

    Mono<Index> getIndex(String indexName);

    Flux<Index> listIndices();

    Mono<Boolean> indexExists(String indexName);
}
