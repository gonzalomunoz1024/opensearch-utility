package com.opensearch.utility.command.cluster.ports.outbound;

import com.opensearch.utility.command.cluster.domain.ClusterHealth;
import com.opensearch.utility.command.cluster.domain.ClusterStats;
import reactor.core.publisher.Mono;

public interface ClusterOperationsPort {

    Mono<ClusterHealth> getClusterHealth();

    Mono<ClusterHealth> getIndexHealth(String indexName);

    Mono<ClusterStats> getClusterStats();
}
