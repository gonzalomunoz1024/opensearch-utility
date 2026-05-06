package com.opensearch.utility.command.scripts.adapters.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opensearch.utility.command.document.domain.Document;
import com.opensearch.utility.command.scripts.exception.MigrationException;
import com.opensearch.utility.command.scripts.ports.outbound.SourceClusterPort;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class SourceOpenSearchAdapter implements SourceClusterPort {

    private final WebClient webClient;

    @Override
    public Mono<Boolean> isReachable() {
        return webClient.get()
                .uri("/_cluster/health")
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(Map.class)
                .map(response -> true)
                .onErrorReturn(false)
                .doOnSuccess(reachable -> log.debug("Source cluster reachable: {}", reachable));
    }

    @Override
    public Flux<Map<String, Object>> listIndices() {
        return webClient.get()
                .uri("/_cat/indices?format=json")
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToFlux(Map.class)
                .map(m -> (Map<String, Object>) m)
                .doOnComplete(() -> log.debug("Listed indices from source cluster"));
    }

    @Override
    public Mono<Map<String, Object>> getIndexSettings(String indexName) {
        return webClient.get()
                .uri("/{index}/_settings", indexName)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(Map.class)
                .map(response -> extractIndexSettings(indexName, (Map<String, Object>) response))
                .doOnSuccess(settings -> log.debug("Retrieved settings for index: {}", indexName));
    }

    @Override
    public Mono<Map<String, Object>> getIndexMappings(String indexName) {
        return webClient.get()
                .uri("/{index}/_mapping", indexName)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(Map.class)
                .map(response -> extractIndexMappings(indexName, (Map<String, Object>) response))
                .doOnSuccess(mappings -> log.debug("Retrieved mappings for index: {}", indexName));
    }

    @Override
    public Flux<Document> scrollDocuments(String indexName, String scrollTimeout, int scrollSize) {
        return initializeScroll(indexName, scrollTimeout, scrollSize)
                .expand(scrollResponse -> {
                    List<Map<String, Object>> hits = scrollResponse.getHits();
                    if (hits == null || hits.isEmpty()) {
                        return clearScroll(scrollResponse.getScrollId())
                                .then(Mono.empty());
                    }
                    return continueScroll(scrollResponse.getScrollId(), scrollTimeout);
                })
                .flatMapIterable(ScrollResponse::getHits)
                .map(this::mapToDocument)
                .doOnSubscribe(s -> log.info("Starting scroll for index: {}", indexName))
                .doOnComplete(() -> log.info("Completed scroll for index: {}", indexName));
    }

    @Override
    public Mono<Long> getDocumentCount(String indexName) {
        return webClient.get()
                .uri("/{index}/_count", indexName)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(CountResponse.class)
                .map(CountResponse::getCount)
                .doOnSuccess(count -> log.debug("Document count for {}: {}", indexName, count));
    }

    @Override
    public Flux<Document> getSavedObjects(String dashboardsIndex, List<String> types) {
        // Build a bool query with should clauses to match both "type" and "type.keyword" fields
        // This handles different OpenSearch Dashboards index mappings
        List<Map<String, Object>> shouldClauses = new ArrayList<>();

        // Match on type field (analyzed)
        Map<String, Object> termsType = new HashMap<>();
        termsType.put("type", types);
        shouldClauses.add(Map.of("terms", termsType));

        // Match on type.keyword field (for exact matching)
        Map<String, Object> termsTypeKeyword = new HashMap<>();
        termsTypeKeyword.put("type.keyword", types);
        shouldClauses.add(Map.of("terms", termsTypeKeyword));

        Map<String, Object> boolQuery = new HashMap<>();
        boolQuery.put("should", shouldClauses);
        boolQuery.put("minimum_should_match", 1);

        Map<String, Object> body = new HashMap<>();
        body.put("query", Map.of("bool", boolQuery));
        body.put("size", 10000);

        log.info("Fetching saved objects from {} with types: {}", dashboardsIndex, types);

        return webClient.post()
                .uri("/{index}/_search", dashboardsIndex)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    if (response.statusCode().value() == 404) {
                        log.warn("Dashboards index {} not found", dashboardsIndex);
                        return Mono.empty();
                    }
                    return handleError(response);
                })
                .bodyToMono(SearchResponse.class)
                .doOnNext(response -> {
                    if (response.getHits() != null && response.getHits().getTotal() != null) {
                        log.info("Found {} saved objects in {}", response.getHits().getTotal().getValue(), dashboardsIndex);
                    }
                })
                .flatMapMany(response -> {
                    if (response.getHits() == null || response.getHits().getHits() == null) {
                        log.warn("No saved objects found in {}", dashboardsIndex);
                        return Flux.empty();
                    }
                    List<HitData> hits = response.getHits().getHits();
                    log.info("Processing {} saved objects from {}", hits.size(), dashboardsIndex);

                    // Log breakdown by type for debugging
                    Map<String, Long> typeCount = new HashMap<>();
                    for (HitData hit : hits) {
                        if (hit.getSource() != null) {
                            String type = (String) hit.getSource().get("type");
                            typeCount.merge(type, 1L, Long::sum);
                        }
                    }
                    log.info("Saved objects by type: {}", typeCount);

                    return Flux.fromIterable(hits);
                })
                .map(this::mapHitDataToDocument)
                .doOnComplete(() -> log.debug("Completed retrieving saved objects from {}", dashboardsIndex));
    }

    private Document mapHitDataToDocument(HitData hit) {
        return Document.builder()
                .id(hit.getId())
                .index(hit.getIndex())
                .source(hit.getSource())
                .build();
    }

    private Mono<ScrollResponse> initializeScroll(String indexName, String scrollTimeout, int scrollSize) {
        Map<String, Object> body = new HashMap<>();
        body.put("size", scrollSize);
        body.put("query", Map.of("match_all", Map.of()));

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/{index}/_search")
                        .queryParam("scroll", scrollTimeout)
                        .build(indexName))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(ScrollApiResponse.class)
                .map(this::mapToScrollResponse);
    }

    private Mono<ScrollResponse> continueScroll(String scrollId, String scrollTimeout) {
        Map<String, Object> body = new HashMap<>();
        body.put("scroll", scrollTimeout);
        body.put("scroll_id", scrollId);

        return webClient.post()
                .uri("/_search/scroll")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(ScrollApiResponse.class)
                .map(this::mapToScrollResponse);
    }

    private Mono<Void> clearScroll(String scrollId) {
        if (scrollId == null || scrollId.isEmpty()) {
            return Mono.empty();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("scroll_id", List.of(scrollId));

        return webClient.delete()
                .uri("/_search/scroll")
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Failed to clear scroll context: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private ScrollResponse mapToScrollResponse(ScrollApiResponse apiResponse) {
        ScrollResponse response = new ScrollResponse();
        response.setScrollId(apiResponse.getScrollId());

        List<Map<String, Object>> hits = new ArrayList<>();
        if (apiResponse.getHits() != null && apiResponse.getHits().getHits() != null) {
            for (var hit : apiResponse.getHits().getHits()) {
                Map<String, Object> hitMap = new HashMap<>();
                hitMap.put("_id", hit.getId());
                hitMap.put("_index", hit.getIndex());
                hitMap.put("_source", hit.getSource());
                hits.add(hitMap);
            }
        }
        response.setHits(hits);
        return response;
    }

    private Document mapToDocument(Map<String, Object> hit) {
        return Document.builder()
                .id((String) hit.get("_id"))
                .index((String) hit.get("_index"))
                .source((Map<String, Object>) hit.get("_source"))
                .build();
    }

    private Map<String, Object> extractIndexSettings(String indexName, Map<String, Object> response) {
        Map<String, Object> indexData = (Map<String, Object>) response.get(indexName);
        if (indexData == null) {
            return Map.of();
        }
        Map<String, Object> settings = (Map<String, Object>) indexData.get("settings");
        if (settings == null) {
            return Map.of();
        }
        Map<String, Object> indexSettings = (Map<String, Object>) settings.get("index");
        if (indexSettings == null) {
            return Map.of();
        }

        // Filter out read-only/system settings that shouldn't be copied
        Map<String, Object> filteredSettings = new HashMap<>();
        if (indexSettings.get("number_of_shards") != null) {
            filteredSettings.put("number_of_shards", indexSettings.get("number_of_shards"));
        }
        if (indexSettings.get("number_of_replicas") != null) {
            filteredSettings.put("number_of_replicas", indexSettings.get("number_of_replicas"));
        }
        if (indexSettings.get("refresh_interval") != null) {
            filteredSettings.put("refresh_interval", indexSettings.get("refresh_interval"));
        }
        if (indexSettings.get("analysis") != null) {
            filteredSettings.put("analysis", indexSettings.get("analysis"));
        }

        return filteredSettings;
    }

    private Map<String, Object> extractIndexMappings(String indexName, Map<String, Object> response) {
        Map<String, Object> indexData = (Map<String, Object>) response.get(indexName);
        if (indexData == null) {
            return Map.of();
        }
        Map<String, Object> mappings = (Map<String, Object>) indexData.get("mappings");
        return mappings != null ? mappings : Map.of();
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("No error body")
                .flatMap(errorBody -> {
                    log.error("Source OpenSearch error [{}]: {}", response.statusCode(), errorBody);
                    return Mono.error(new MigrationException.SourceClusterUnreachableException(
                            "Request failed with status " + response.statusCode() + ": " + errorBody));
                });
    }

    @Data
    private static class ScrollResponse {
        private String scrollId;
        private List<Map<String, Object>> hits = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ScrollApiResponse {
        @JsonProperty("_scroll_id")
        private String scrollId;
        private HitsWrapper hits;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SearchResponse {
        private HitsWrapper hits;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class HitsWrapper {
        private List<HitData> hits;
        private TotalHits total;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TotalHits {
        private long value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class HitData {
        @JsonProperty("_id")
        private String id;
        @JsonProperty("_index")
        private String index;
        @JsonProperty("_source")
        private Map<String, Object> source;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CountResponse {
        private long count;
    }
}
