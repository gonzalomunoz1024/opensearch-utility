package com.opensearch.utility.integration;

import com.opensearch.utility.command.cluster.domain.ClusterStats;
import com.opensearch.utility.command.cluster.domain.dto.outbound.ClusterHealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

class ClusterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldGetClusterHealth() {
        webTestClient.get()
                .uri("/api/v1/cluster/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ClusterHealthResponse.class)
                .value(response -> {
                    assertThat(response.getClusterName()).isNotBlank();
                    assertThat(response.getStatus()).isIn("green", "yellow", "red");
                    assertThat(response.getNumberOfNodes()).isGreaterThan(0);
                });
    }

    @Test
    void shouldGetClusterStats() {
        webTestClient.get()
                .uri("/api/v1/cluster/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ClusterStats.class)
                .value(stats -> {
                    assertThat(stats.getClusterName()).isNotBlank();
                    assertThat(stats.getNodes()).isNotNull();
                    assertThat(stats.getNodes().getTotal()).isGreaterThan(0);
                });
    }
}
