package com.opensearch.utility.command.cluster.ports.inbound;

import com.opensearch.utility.command.cluster.domain.ClusterHealth;
import reactor.core.publisher.Mono;

public interface GetClusterHealthPort {

    Mono<ClusterHealth> getClusterHealth();

    Mono<ClusterHealth> getIndexHealth(String indexName);
}
