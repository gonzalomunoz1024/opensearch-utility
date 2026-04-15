package com.opensearch.utility.command.document.adapters.outbound;

import com.opensearch.utility.command.document.domain.Document;
import com.opensearch.utility.command.document.ports.outbound.CustomerDataSourcePort;
import com.opensearch.utility.core.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@Component
public class CustomerRestApiAdapter implements CustomerDataSourcePort {

    private final WebClient.Builder webClientBuilder;
    private final IdGenerator idGenerator;

    public CustomerRestApiAdapter(WebClient.Builder webClientBuilder, IdGenerator idGenerator) {
        this.webClientBuilder = webClientBuilder;
        this.idGenerator = idGenerator;
    }

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
        String id = idGenerator.extractOrGenerate(data);
        return Document.builder()
                .id(id)
                .source(data)
                .build();
    }
}
