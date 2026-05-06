package com.opensearch.utility.command.scripts.ports.outbound;

import com.opensearch.utility.command.document.domain.Document;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface SourceClusterPort {

    Mono<Boolean> isReachable();

    Flux<Map<String, Object>> listIndices();

    Mono<Map<String, Object>> getIndexSettings(String indexName);

    Mono<Map<String, Object>> getIndexMappings(String indexName);

    Flux<Document> scrollDocuments(String indexName, String scrollTimeout, int scrollSize);

    Mono<Long> getDocumentCount(String indexName);

    Flux<Document> getSavedObjects(String dashboardsIndex, List<String> types);

    /**
     * Find all indices matching the OpenSearch Dashboards pattern (e.g., .opensearch_dashboards*, .kibana*)
     */
    Flux<String> findDashboardsIndices();

    /**
     * Get all documents from an index (for debugging saved objects)
     */
    Flux<Document> getAllDocuments(String indexName, int maxSize);
}
