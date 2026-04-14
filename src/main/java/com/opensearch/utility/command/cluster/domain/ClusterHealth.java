package com.opensearch.utility.command.cluster.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterHealth {

    private String clusterName;
    private ClusterStatus status;
    private boolean timedOut;
    private int numberOfNodes;
    private int numberOfDataNodes;
    private int activePrimaryShards;
    private int activeShards;
    private int relocatingShards;
    private int initializingShards;
    private int unassignedShards;
    private int delayedUnassignedShards;
    private int numberOfPendingTasks;
    private int numberOfInFlightFetch;
    private long taskMaxWaitingInQueueMillis;
    private double activeShardsPercentAsNumber;

    public enum ClusterStatus {
        GREEN, YELLOW, RED
    }

    public boolean isHealthy() {
        return status == ClusterStatus.GREEN;
    }

    public boolean hasUnassignedShards() {
        return unassignedShards > 0;
    }
}
