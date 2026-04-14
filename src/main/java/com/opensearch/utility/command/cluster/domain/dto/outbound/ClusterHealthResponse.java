package com.opensearch.utility.command.cluster.domain.dto.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opensearch.utility.command.cluster.domain.ClusterHealth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterHealthResponse {

    private String clusterName;
    private String status;
    private boolean timedOut;
    private int numberOfNodes;
    private int numberOfDataNodes;
    private int activePrimaryShards;
    private int activeShards;
    private int relocatingShards;
    private int initializingShards;
    private int unassignedShards;
    private int numberOfPendingTasks;
    private double activeShardsPercentAsNumber;
    private boolean healthy;

    public static ClusterHealthResponse from(ClusterHealth health) {
        return ClusterHealthResponse.builder()
                .clusterName(health.getClusterName())
                .status(health.getStatus() != null ? health.getStatus().name().toLowerCase() : null)
                .timedOut(health.isTimedOut())
                .numberOfNodes(health.getNumberOfNodes())
                .numberOfDataNodes(health.getNumberOfDataNodes())
                .activePrimaryShards(health.getActivePrimaryShards())
                .activeShards(health.getActiveShards())
                .relocatingShards(health.getRelocatingShards())
                .initializingShards(health.getInitializingShards())
                .unassignedShards(health.getUnassignedShards())
                .numberOfPendingTasks(health.getNumberOfPendingTasks())
                .activeShardsPercentAsNumber(health.getActiveShardsPercentAsNumber())
                .healthy(health.isHealthy())
                .build();
    }
}
