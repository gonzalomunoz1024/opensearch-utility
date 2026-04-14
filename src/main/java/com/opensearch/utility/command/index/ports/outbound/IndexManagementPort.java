package com.opensearch.utility.command.index.ports.outbound;

import com.opensearch.utility.command.index.domain.Index;
import com.opensearch.utility.command.index.domain.IndexSettings;
import com.opensearch.utility.command.index.domain.command.ReindexCommand;
import com.opensearch.utility.command.index.domain.dto.outbound.ReindexResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface IndexManagementPort {

    Mono<Index> createIndex(String indexName, IndexSettings settings, Map<String, Object> mappings);

    Mono<Void> deleteIndex(String indexName);

    Mono<Index> getIndex(String indexName);

    Flux<Index> listIndices();

    Mono<Boolean> indexExists(String indexName);

    Mono<Void> refreshIndex(String indexName);

    Mono<Void> openIndex(String indexName);

    Mono<Void> closeIndex(String indexName);

    Mono<ReindexResponse> reindex(ReindexCommand command);
}
