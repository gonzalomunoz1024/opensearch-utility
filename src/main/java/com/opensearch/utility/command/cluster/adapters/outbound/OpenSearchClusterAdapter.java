package com.opensearch.utility.command.cluster.adapters.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opensearch.utility.command.cluster.domain.ClusterHealth;
import com.opensearch.utility.command.cluster.domain.ClusterStats;
import com.opensearch.utility.command.cluster.ports.outbound.ClusterOperationsPort;
import com.opensearch.utility.core.http.OpenSearchHttpClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSearchClusterAdapter implements ClusterOperationsPort {

    private final OpenSearchHttpClient httpClient;

    @Override
    public Mono<ClusterHealth> getClusterHealth() {
        return httpClient.get("/_cluster/health", ClusterHealthApiResponse.class)
                .map(this::mapToClusterHealth)
                .doOnSuccess(health -> log.debug("Cluster health: {}", health.getStatus()));
    }

    @Override
    public Mono<ClusterHealth> getIndexHealth(String indexName) {
        return httpClient.get("/_cluster/health/" + indexName, ClusterHealthApiResponse.class)
                .map(this::mapToClusterHealth);
    }

    @Override
    public Mono<ClusterStats> getClusterStats() {
        return httpClient.get("/_cluster/stats", Map.class)
                .map(this::mapToClusterStats);
    }

    private ClusterHealth mapToClusterHealth(ClusterHealthApiResponse response) {
        ClusterHealth.ClusterStatus status = null;
        if (response.getStatus() != null) {
            status = ClusterHealth.ClusterStatus.valueOf(response.getStatus().toUpperCase());
        }

        return ClusterHealth.builder()
                .clusterName(response.getClusterName())
                .status(status)
                .timedOut(response.isTimedOut())
                .numberOfNodes(response.getNumberOfNodes())
                .numberOfDataNodes(response.getNumberOfDataNodes())
                .activePrimaryShards(response.getActivePrimaryShards())
                .activeShards(response.getActiveShards())
                .relocatingShards(response.getRelocatingShards())
                .initializingShards(response.getInitializingShards())
                .unassignedShards(response.getUnassignedShards())
                .delayedUnassignedShards(response.getDelayedUnassignedShards())
                .numberOfPendingTasks(response.getNumberOfPendingTasks())
                .numberOfInFlightFetch(response.getNumberOfInFlightFetch())
                .taskMaxWaitingInQueueMillis(response.getTaskMaxWaitingInQueueMillis())
                .activeShardsPercentAsNumber(response.getActiveShardsPercentAsNumber())
                .build();
    }

    @SuppressWarnings("unchecked")
    private ClusterStats mapToClusterStats(Map<String, Object> response) {
        String clusterName = (String) response.get("cluster_name");
        String clusterUuid = (String) response.get("cluster_uuid");
        Object timestampObj = response.get("timestamp");
        long timestamp = timestampObj instanceof Number ? ((Number) timestampObj).longValue() : 0;

        String statusStr = (String) response.get("status");
        ClusterStats.ClusterStatus status = statusStr != null
                ? ClusterStats.ClusterStatus.valueOf(statusStr.toUpperCase())
                : null;

        ClusterStats.IndicesStats indicesStats = null;
        Map<String, Object> indices = (Map<String, Object>) response.get("indices");
        if (indices != null) {
            int count = indices.get("count") != null ? ((Number) indices.get("count")).intValue() : 0;

            ClusterStats.ShardsStats shardsStats = null;
            Map<String, Object> shards = (Map<String, Object>) indices.get("shards");
            if (shards != null) {
                shardsStats = ClusterStats.ShardsStats.builder()
                        .total(shards.get("total") != null ? ((Number) shards.get("total")).intValue() : 0)
                        .primaries(shards.get("primaries") != null ? ((Number) shards.get("primaries")).intValue() : 0)
                        .replication(shards.get("replication") != null ? ((Number) shards.get("replication")).doubleValue() : 0)
                        .build();
            }

            ClusterStats.DocsStats docsStats = null;
            Map<String, Object> docs = (Map<String, Object>) indices.get("docs");
            if (docs != null) {
                docsStats = ClusterStats.DocsStats.builder()
                        .count(docs.get("count") != null ? ((Number) docs.get("count")).longValue() : 0)
                        .deleted(docs.get("deleted") != null ? ((Number) docs.get("deleted")).longValue() : 0)
                        .build();
            }

            ClusterStats.StoreStats storeStats = null;
            Map<String, Object> store = (Map<String, Object>) indices.get("store");
            if (store != null) {
                storeStats = ClusterStats.StoreStats.builder()
                        .sizeInBytes(store.get("size_in_bytes") != null ? ((Number) store.get("size_in_bytes")).longValue() : 0)
                        .build();
            }

            indicesStats = ClusterStats.IndicesStats.builder()
                    .count(count)
                    .shards(shardsStats)
                    .docs(docsStats)
                    .store(storeStats)
                    .build();
        }

        ClusterStats.NodesStats nodesStats = null;
        Map<String, Object> nodesObj = (Map<String, Object>) response.get("_nodes");
        if (nodesObj != null) {
            nodesStats = ClusterStats.NodesStats.builder()
                    .total(nodesObj.get("total") != null ? ((Number) nodesObj.get("total")).intValue() : 0)
                    .successful(nodesObj.get("successful") != null ? ((Number) nodesObj.get("successful")).intValue() : 0)
                    .failed(nodesObj.get("failed") != null ? ((Number) nodesObj.get("failed")).intValue() : 0)
                    .build();
        }

        return ClusterStats.builder()
                .clusterName(clusterName)
                .clusterUuid(clusterUuid)
                .timestamp(timestamp)
                .status(status)
                .indices(indicesStats)
                .nodes(nodesStats)
                .build();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ClusterHealthApiResponse {
        @JsonProperty("cluster_name")
        private String clusterName;
        private String status;
        @JsonProperty("timed_out")
        private boolean timedOut;
        @JsonProperty("number_of_nodes")
        private int numberOfNodes;
        @JsonProperty("number_of_data_nodes")
        private int numberOfDataNodes;
        @JsonProperty("active_primary_shards")
        private int activePrimaryShards;
        @JsonProperty("active_shards")
        private int activeShards;
        @JsonProperty("relocating_shards")
        private int relocatingShards;
        @JsonProperty("initializing_shards")
        private int initializingShards;
        @JsonProperty("unassigned_shards")
        private int unassignedShards;
        @JsonProperty("delayed_unassigned_shards")
        private int delayedUnassignedShards;
        @JsonProperty("number_of_pending_tasks")
        private int numberOfPendingTasks;
        @JsonProperty("number_of_in_flight_fetch")
        private int numberOfInFlightFetch;
        @JsonProperty("task_max_waiting_in_queue_millis")
        private long taskMaxWaitingInQueueMillis;
        @JsonProperty("active_shards_percent_as_number")
        private double activeShardsPercentAsNumber;
    }
}
