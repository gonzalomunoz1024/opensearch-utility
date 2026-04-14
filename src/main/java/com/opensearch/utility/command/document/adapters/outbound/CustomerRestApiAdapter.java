package com.opensearch.utility.command.document.adapters.outbound;

import com.opensearch.utility.command.document.domain.Document;
import com.opensearch.utility.command.document.ports.outbound.CustomerDataSourcePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerRestApiAdapter implements CustomerDataSourcePort {

    private final WebClient.Builder webClientBuilder;

    @Override
    public Flux<Document> fetchDocuments(String sourceEndpoint) {
        log.info("Fetching documents from: {}", sourceEndpoint);

        WebClient client = webClientBuilder.build();

        return client.get()
                .uri(sourceEndpoint)
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToDocument)
                .doOnNext(doc -> log.debug("Fetched document: {}", doc.getId()))
                .doOnComplete(() -> log.info("Completed fetching documents from: {}", sourceEndpoint))
                .doOnError(e -> log.error("Error fetching documents from {}: {}", sourceEndpoint, e.getMessage()));
    }

    @SuppressWarnings("unchecked")
    private Document mapToDocument(Map<String, Object> data) {
        String id = extractId(data);
        return Document.builder()
                .id(id)
                .source(data)
                .build();
    }

    private String extractId(Map<String, Object> data) {
        // Try common ID field names
        Object id = data.get("id");
        if (id != null) return id.toString();

        id = data.get("_id");
        if (id != null) return id.toString();

        id = data.get("documentId");
        if (id != null) return id.toString();

        // Generate UUID if no ID found
        return UUID.randomUUID().toString();
    }
}
