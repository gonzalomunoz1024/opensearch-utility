package com.opensearch.utility.command.cluster.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterStats {

    private String clusterName;
    private String clusterUuid;
    private long timestamp;
    private ClusterStatus status;
    private IndicesStats indices;
    private NodesStats nodes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicesStats {
        private int count;
        private ShardsStats shards;
        private DocsStats docs;
        private StoreStats store;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShardsStats {
        private int total;
        private int primaries;
        private double replication;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocsStats {
        private long count;
        private long deleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreStats {
        private long sizeInBytes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodesStats {
        private int total;
        private int successful;
        private int failed;
    }

    public enum ClusterStatus {
        GREEN, YELLOW, RED
    }
}
