package com.opensearch.utility.command.index.ports.inbound;

import com.opensearch.utility.command.index.domain.Index;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IndexQueryPort {

    Mono<Index> getIndex(String indexName);

    Flux<Index> listIndices();

    Mono<Boolean> indexExists(String indexName);
}
