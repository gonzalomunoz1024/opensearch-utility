package com.opensearch.utility.command.scripts.adapters.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearch.utility.command.document.domain.BulkOperationResult;
import com.opensearch.utility.command.document.domain.Document;
import com.opensearch.utility.command.scripts.exception.MigrationException;
import com.opensearch.utility.command.scripts.ports.outbound.TargetClusterPort;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class TargetOpenSearchAdapter implements TargetClusterPort {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Boolean> isReachable() {
        return webClient.get()
                .uri("/_cluster/health")
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(Map.class)
                .map(response -> true)
                .onErrorReturn(false)
                .doOnSuccess(reachable -> log.debug("Target cluster reachable: {}", reachable));
    }

    @Override
    public Mono<Boolean> indexExists(String indexName) {
        return webClient.head()
                .uri("/{index}", indexName)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.just(true);
                    } else if (response.statusCode().value() == 404) {
                        return Mono.just(false);
                    }
                    return handleError(response).then(Mono.just(false));
                })
                .doOnSuccess(exists -> log.debug("Index {} exists in target: {}", indexName, exists));
    }

    @Override
    public Mono<Void> createIndex(String indexName, Map<String, Object> settings, Map<String, Object> mappings) {
        Map<String, Object> body = new HashMap<>();

        if (settings != null && !settings.isEmpty()) {
            body.put("settings", Map.of("index", settings));
        }
        if (mappings != null && !mappings.isEmpty()) {
            body.put("mappings", mappings);
        }

        return webClient.put()
                .uri("/{index}", indexName)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(Map.class)
                .then()
                .doOnSuccess(v -> log.info("Created index in target: {}", indexName));
    }

    @Override
    public Mono<BulkOperationResult> bulkIndex(String indexName, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return Mono.just(BulkOperationResult.builder()
                    .took(0)
                    .errors(false)
                    .items(List.of())
                    .build());
        }

        String bulkBody = buildBulkRequestBody(indexName, documents);

        return webClient.post()
                .uri("/_bulk")
                .contentType(MediaType.valueOf("application/x-ndjson"))
                .bodyValue(bulkBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(BulkResponse.class)
                .map(response -> mapToBulkOperationResult(response, documents))
                .doOnSuccess(result -> log.debug("Bulk indexed {} docs to {}, errors: {}",
                        documents.size(), indexName, result.isErrors()));
    }

    @Override
    public Mono<Void> refreshIndex(String indexName) {
        return webClient.post()
                .uri("/{index}/_refresh", indexName)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(Map.class)
                .then()
                .doOnSuccess(v -> log.debug("Refreshed index: {}", indexName));
    }

    private String buildBulkRequestBody(String indexName, List<Document> documents) {
        StringBuilder builder = new StringBuilder();
        for (Document doc : documents) {
            String docId = doc.getId() != null ? doc.getId() : UUID.randomUUID().toString();

            // Action line
            builder.append("{\"index\":{\"_index\":\"")
                    .append(indexName)
                    .append("\",\"_id\":\"")
                    .append(docId)
                    .append("\"}}\n");

            // Document source line
            builder.append(toJson(doc.getSource())).append("\n");
        }
        return builder.toString();
    }

    private String toJson(Map<String, Object> source) {
        if (source == null) return "{}";
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize document source: {}", e.getMessage());
            return "{}";
        }
    }

    private BulkOperationResult mapToBulkOperationResult(BulkResponse response, List<Document> documents) {
        List<BulkOperationResult.BulkItemResult> items = new ArrayList<>();

        if (response.getItems() != null) {
            for (Map<String, Object> item : response.getItems()) {
                Map<String, Object> indexResult = (Map<String, Object>) item.get("index");

                if (indexResult != null) {
                    String errorType = null;
                    String errorReason = null;
                    Map<String, Object> error = (Map<String, Object>) indexResult.get("error");
                    if (error != null) {
                        errorType = (String) error.get("type");
                        errorReason = (String) error.get("reason");
                    }

                    Integer status = (Integer) indexResult.get("status");
                    boolean success = status != null && status >= 200 && status < 300;

                    items.add(BulkOperationResult.BulkItemResult.builder()
                            .id((String) indexResult.get("_id"))
                            .index((String) indexResult.get("_index"))
                            .result((String) indexResult.get("result"))
                            .status(status != null ? status : 0)
                            .success(success)
                            .errorType(errorType)
                            .errorReason(errorReason)
                            .build());
                }
            }
        }

        return BulkOperationResult.builder()
                .took(response.getTook())
                .errors(response.isErrors())
                .items(items)
                .build();
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("No error body")
                .flatMap(errorBody -> {
                    log.error("Target OpenSearch error [{}]: {}", response.statusCode(), errorBody);
                    return Mono.error(new MigrationException.TargetClusterUnreachableException(
                            "Request failed with status " + response.statusCode() + ": " + errorBody));
                });
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BulkResponse {
        private long took;
        private boolean errors;
        private List<Map<String, Object>> items;
    }
}
