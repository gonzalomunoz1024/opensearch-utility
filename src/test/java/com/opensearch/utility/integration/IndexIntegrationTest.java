package com.opensearch.utility.integration;

import com.opensearch.utility.command.index.domain.IndexSettings;
import com.opensearch.utility.command.index.domain.dto.inbound.CreateIndexRequest;
import com.opensearch.utility.command.index.domain.dto.outbound.IndexResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

class IndexIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateIndex() {
        String indexName = "test-index-" + System.currentTimeMillis();

        CreateIndexRequest request = CreateIndexRequest.builder()
                .indexName(indexName)
                .settings(IndexSettings.builder()
                        .numberOfShards(1)
                        .numberOfReplicas(0)
                        .build())
                .build();

        webTestClient.post()
                .uri("/api/v1/indices")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(IndexResponse.class)
                .value(response -> {
                    assertThat(response.getName()).isEqualTo(indexName);
                });

        // Cleanup
        webTestClient.delete()
                .uri("/api/v1/indices/{indexName}", indexName)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void shouldListIndices() {
        webTestClient.get()
                .uri("/api/v1/indices")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(IndexResponse.class);
    }

    @Test
    void shouldReturn404ForNonExistentIndex() {
        webTestClient.get()
                .uri("/api/v1/indices/{indexName}", "non-existent-index")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn409ForDuplicateIndex() {
        String indexName = "duplicate-test-" + System.currentTimeMillis();

        CreateIndexRequest request = CreateIndexRequest.builder()
                .indexName(indexName)
                .build();

        // Create first time
        webTestClient.post()
                .uri("/api/v1/indices")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Try to create again
        webTestClient.post()
                .uri("/api/v1/indices")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);

        // Cleanup
        webTestClient.delete()
                .uri("/api/v1/indices/{indexName}", indexName)
                .exchange()
                .expectStatus().isNoContent();
    }
}
