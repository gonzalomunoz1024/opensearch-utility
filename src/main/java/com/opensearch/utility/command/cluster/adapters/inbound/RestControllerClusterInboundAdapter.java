package com.opensearch.utility.command.cluster.adapters.inbound;

import com.opensearch.utility.command.cluster.domain.ClusterStats;
import com.opensearch.utility.command.cluster.domain.dto.outbound.ClusterHealthResponse;
import com.opensearch.utility.command.cluster.usecases.GetClusterHealthUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/cluster")
@RequiredArgsConstructor
public class RestControllerClusterInboundAdapter {

    private final GetClusterHealthUseCase getClusterHealthUseCase;

    @GetMapping("/health")
    public Mono<ResponseEntity<ClusterHealthResponse>> getClusterHealth() {
        log.debug("REST: Get cluster health");
        return getClusterHealthUseCase.getClusterHealth()
                .map(ClusterHealthResponse::from)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/health/{indexName}")
    public Mono<ResponseEntity<ClusterHealthResponse>> getIndexHealth(@PathVariable String indexName) {
        log.debug("REST: Get index health for: {}", indexName);
        return getClusterHealthUseCase.getIndexHealth(indexName)
                .map(ClusterHealthResponse::from)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/stats")
    public Mono<ResponseEntity<ClusterStats>> getClusterStats() {
        log.debug("REST: Get cluster stats");
        return getClusterHealthUseCase.getClusterStats()
                .map(ResponseEntity::ok);
    }
}
