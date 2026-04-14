package com.opensearch.utility.command.document.ports.outbound;

import com.opensearch.utility.command.document.domain.BulkOperationResult;
import com.opensearch.utility.command.document.domain.Document;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DocumentPersistencePort {

    Mono<BulkOperationResult> bulkSave(String indexName, List<Document> documents);

    Mono<Document> save(String indexName, Document document);

    Mono<Document> getById(String indexName, String documentId);

    Mono<Void> delete(String indexName, String documentId);
}
