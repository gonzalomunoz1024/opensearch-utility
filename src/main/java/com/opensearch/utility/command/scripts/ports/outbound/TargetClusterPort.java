package com.opensearch.utility.command.scripts.ports.outbound;

import com.opensearch.utility.command.document.domain.BulkOperationResult;
import com.opensearch.utility.command.document.domain.Document;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface TargetClusterPort {

    Mono<Boolean> isReachable();

    Mono<Boolean> indexExists(String indexName);

    Mono<Void> createIndex(String indexName, Map<String, Object> settings, Map<String, Object> mappings);

    Mono<BulkOperationResult> bulkIndex(String indexName, List<Document> documents);

    Mono<Void> refreshIndex(String indexName);
}
