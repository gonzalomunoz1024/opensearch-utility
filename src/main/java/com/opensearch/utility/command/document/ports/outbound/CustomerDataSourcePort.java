package com.opensearch.utility.command.document.ports.outbound;

import com.opensearch.utility.command.document.domain.Document;
import reactor.core.publisher.Flux;

public interface CustomerDataSourcePort {

    Flux<Document> fetchDocuments(String sourceEndpoint);
}
