package com.opensearch.utility.command.index.adapters.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opensearch.utility.command.index.domain.Index;
import com.opensearch.utility.command.index.domain.IndexSettings;
import com.opensearch.utility.command.index.domain.command.ReindexCommand;
import com.opensearch.utility.command.index.domain.dto.outbound.ReindexResponse;
import com.opensearch.utility.command.index.ports.outbound.IndexManagementPort;
import com.opensearch.utility.core.http.OpenSearchHttpClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSearchIndexAdapter implements IndexManagementPort {

    private final OpenSearchHttpClient httpClient;

    @Override
    public Mono<Index> createIndex(String indexName, IndexSettings settings, Map<String, Object> mappings) {
        Map<String, Object> body = new HashMap<>();
        if (settings != null) {
            body.put("settings", settings);
        }
        if (mappings != null) {
            body.put("mappings", mappings);
        }

        return httpClient.put("/" + indexName, body, CreateIndexResponse.class)
                .map(response -> Index.builder()
                        .name(indexName)
                        .uuid(response.getIndex())
                        .settings(settings)
                        .build())
                .doOnSuccess(idx -> log.info("Created index: {}", indexName));
    }

    @Override
    public Mono<Void> deleteIndex(String indexName) {
        return httpClient.delete("/" + indexName)
                .doOnSuccess(v -> log.info("Deleted index: {}", indexName));
    }

    @Override
    public Mono<Index> getIndex(String indexName) {
        return httpClient.get("/" + indexName, Map.class)
                .map(response -> mapToIndex(indexName, response))
                .doOnError(e -> log.warn("Failed to get index {}: {}", indexName, e.getMessage()));
    }

    @Override
    public Flux<Index> listIndices() {
        return httpClient.get("/_cat/indices?format=json", List.class)
                .flatMapMany(indices -> Flux.fromIterable((List<Map<String, Object>>) indices))
                .map(this::mapCatIndexToIndex);
    }

    @Override
    public Mono<Boolean> indexExists(String indexName) {
        return httpClient.head("/" + indexName);
    }

    @Override
    public Mono<Void> refreshIndex(String indexName) {
        return httpClient.post("/" + indexName + "/_refresh", null, Map.class)
                .then()
                .doOnSuccess(v -> log.debug("Refreshed index: {}", indexName));
    }

    @Override
    public Mono<Void> openIndex(String indexName) {
        return httpClient.post("/" + indexName + "/_open", null, Map.class)
                .then()
                .doOnSuccess(v -> log.info("Opened index: {}", indexName));
    }

    @Override
    public Mono<Void> closeIndex(String indexName) {
        return httpClient.post("/" + indexName + "/_close", null, Map.class)
                .then()
                .doOnSuccess(v -> log.info("Closed index: {}", indexName));
    }

    @Override
    public Mono<ReindexResponse> reindex(ReindexCommand command) {
        Map<String, Object> body = buildReindexBody(command);

        return httpClient.post("/_reindex?refresh=" + command.isRefresh(), body, ReindexApiResponse.class)
                .map(this::mapToReindexResponse)
                .doOnSuccess(response -> log.info("Reindex completed: {} documents from {} to {}",
                        response.getTotal(), command.getSourceIndex(), command.getDestinationIndex()));
    }

    private Map<String, Object> buildReindexBody(ReindexCommand command) {
        Map<String, Object> body = new HashMap<>();

        // Source configuration
        Map<String, Object> source = new HashMap<>();
        source.put("index", command.getSourceIndex());
        if (command.getQuery() != null && !command.getQuery().isEmpty()) {
            source.put("query", command.getQuery());
        }
        if (command.getBatchSize() > 0) {
            source.put("size", command.getBatchSize());
        }
        body.put("source", source);

        // Destination configuration
        Map<String, Object> dest = new HashMap<>();
        dest.put("index", command.getDestinationIndex());
        body.put("dest", dest);

        // Optional script for transformation
        if (command.getScript() != null && !command.getScript().isEmpty()) {
            Map<String, Object> script = new HashMap<>();
            script.put("source", command.getScript());
            if (command.getScriptParams() != null && !command.getScriptParams().isEmpty()) {
                script.put("params", command.getScriptParams());
            }
            body.put("script", script);
        }

        // Conflict handling
        if (command.getConflicts() != null && !command.getConflicts().isEmpty()) {
            body.put("conflicts", command.getConflicts());
        }

        return body;
    }

    private ReindexResponse mapToReindexResponse(ReindexApiResponse apiResponse) {
        return ReindexResponse.builder()
                .took(apiResponse.getTook())
                .timedOut(apiResponse.isTimedOut())
                .total(apiResponse.getTotal())
                .created(apiResponse.getCreated())
                .updated(apiResponse.getUpdated())
                .deleted(apiResponse.getDeleted())
                .batches(apiResponse.getBatches())
                .versionConflicts(apiResponse.getVersionConflicts())
                .noops(apiResponse.getNoops())
                .retries(apiResponse.getRetries() != null
                        ? ReindexResponse.RetryStats.builder()
                        .bulk(apiResponse.getRetries().getBulk())
                        .search(apiResponse.getRetries().getSearch())
                        .build()
                        : null)
                .failures(apiResponse.getFailures() != null
                        ? apiResponse.getFailures()
                        : Collections.emptyList())
                .build();
    }

    private Index mapToIndex(String indexName, Map<String, Object> response) {
        Map<String, Object> indexData = (Map<String, Object>) response.get(indexName);
        if (indexData == null) {
            return Index.builder().name(indexName).build();
        }

        Map<String, Object> settingsData = (Map<String, Object>) indexData.get("settings");
        Map<String, Object> indexSettings = settingsData != null
                ? (Map<String, Object>) settingsData.get("index")
                : null;

        IndexSettings settings = null;
        if (indexSettings != null) {
            settings = IndexSettings.builder()
                    .numberOfShards(parseInteger(indexSettings.get("number_of_shards")))
                    .numberOfReplicas(parseInteger(indexSettings.get("number_of_replicas")))
                    .build();
        }

        return Index.builder()
                .name(indexName)
                .uuid(indexSettings != null ? (String) indexSettings.get("uuid") : null)
                .settings(settings)
                .build();
    }

    private Index mapCatIndexToIndex(Map<String, Object> catIndex) {
        String healthStr = (String) catIndex.get("health");
        Index.IndexHealth health = healthStr != null
                ? Index.IndexHealth.valueOf(healthStr.toUpperCase())
                : null;

        return Index.builder()
                .name((String) catIndex.get("index"))
                .uuid((String) catIndex.get("uuid"))
                .health(health)
                .docsCount(parseLong(catIndex.get("docs.count")))
                .storeSizeBytes(parseStoreSizeToBytes((String) catIndex.get("store.size")))
                .build();
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        return Integer.parseInt(value.toString());
    }

    private long parseLong(Object value) {
        if (value == null) return 0;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseStoreSizeToBytes(String storeSize) {
        if (storeSize == null || storeSize.isEmpty()) return 0;
        try {
            storeSize = storeSize.toLowerCase().trim();
            double value;
            long multiplier;

            if (storeSize.endsWith("kb")) {
                value = Double.parseDouble(storeSize.replace("kb", ""));
                multiplier = 1024;
            } else if (storeSize.endsWith("mb")) {
                value = Double.parseDouble(storeSize.replace("mb", ""));
                multiplier = 1024 * 1024;
            } else if (storeSize.endsWith("gb")) {
                value = Double.parseDouble(storeSize.replace("gb", ""));
                multiplier = 1024 * 1024 * 1024;
            } else if (storeSize.endsWith("b")) {
                value = Double.parseDouble(storeSize.replace("b", ""));
                multiplier = 1;
            } else {
                value = Double.parseDouble(storeSize);
                multiplier = 1;
            }
            return (long) (value * multiplier);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CreateIndexResponse {
        private boolean acknowledged;
        @JsonProperty("shards_acknowledged")
        private boolean shardsAcknowledged;
        private String index;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ReindexApiResponse {
        private long took;
        @JsonProperty("timed_out")
        private boolean timedOut;
        private long total;
        private long created;
        private long updated;
        private long deleted;
        private int batches;
        @JsonProperty("version_conflicts")
        private long versionConflicts;
        private long noops;
        private RetriesData retries;
        private List<String> failures;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class RetriesData {
            private int bulk;
            private int search;
        }
    }
}
