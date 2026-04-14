package com.opensearch.utility.command.cluster.usecases;

import com.opensearch.utility.command.cluster.domain.ClusterHealth;
import com.opensearch.utility.command.cluster.domain.ClusterStats;
import com.opensearch.utility.command.cluster.domain.event.ClusterHealthCheckedEvent;
import com.opensearch.utility.command.cluster.ports.inbound.GetClusterHealthPort;
import com.opensearch.utility.command.cluster.ports.outbound.ClusterOperationsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultGetClusterHealthUseCase implements GetClusterHealthUseCase, GetClusterHealthPort {

    private final ClusterOperationsPort clusterOperationsPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Mono<ClusterHealth> getClusterHealth() {
        return clusterOperationsPort.getClusterHealth()
                .doOnSuccess(health -> {
                    publishHealthCheckedEvent(health, false);
                    logHealthStatus(health);
                });
    }

    @Override
    public Mono<ClusterHealth> getIndexHealth(String indexName) {
        return clusterOperationsPort.getIndexHealth(indexName);
    }

    @Override
    public Mono<ClusterStats> getClusterStats() {
        return clusterOperationsPort.getClusterStats();
    }

    public Mono<ClusterHealth> checkHealthScheduled() {
        return clusterOperationsPort.getClusterHealth()
                .doOnSuccess(health -> {
                    publishHealthCheckedEvent(health, true);
                    logHealthStatus(health);
                });
    }

    private void publishHealthCheckedEvent(ClusterHealth health, boolean scheduled) {
        ClusterHealthCheckedEvent event = ClusterHealthCheckedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .correlationId(UUID.randomUUID().toString())
                .clusterHealth(health)
                .scheduledCheck(scheduled)
                .build();
        eventPublisher.publishEvent(event);
    }

    private void logHealthStatus(ClusterHealth health) {
        if (health.getStatus() == ClusterHealth.ClusterStatus.RED) {
            log.error("Cluster health is RED: {} - nodes: {}, unassigned shards: {}",
                    health.getClusterName(), health.getNumberOfNodes(), health.getUnassignedShards());
        } else if (health.getStatus() == ClusterHealth.ClusterStatus.YELLOW) {
            log.warn("Cluster health is YELLOW: {} - nodes: {}, unassigned shards: {}",
                    health.getClusterName(), health.getNumberOfNodes(), health.getUnassignedShards());
        } else {
            log.info("Cluster health is GREEN: {} - nodes: {}, active shards: {}",
                    health.getClusterName(), health.getNumberOfNodes(), health.getActiveShards());
        }
    }
}
