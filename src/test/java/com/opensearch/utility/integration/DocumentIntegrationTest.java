package com.opensearch.utility.integration;

import com.opensearch.utility.command.document.domain.dto.inbound.BulkDocumentRequest;
import com.opensearch.utility.command.document.domain.dto.outbound.BulkResultResponse;
import com.opensearch.utility.command.index.domain.dto.inbound.CreateIndexRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private String testIndexName;

    @BeforeEach
    void setUp() {
        testIndexName = "doc-test-" + System.currentTimeMillis();

        CreateIndexRequest createRequest = CreateIndexRequest.builder()
                .indexName(testIndexName)
                .build();

        webTestClient.post()
                .uri("/api/v1/indices")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated();
    }

    @AfterEach
    void tearDown() {
        webTestClient.delete()
                .uri("/api/v1/indices/{indexName}", testIndexName)
                .exchange();
    }

    @Test
    void shouldBulkIndexDocuments() {
        List<Map<String, Object>> documents = List.of(
                Map.of("id", "1", "name", "Document 1", "value", 100),
                Map.of("id", "2", "name", "Document 2", "value", 200),
                Map.of("id", "3", "name", "Document 3", "value", 300)
        );

        BulkDocumentRequest request = BulkDocumentRequest.builder()
                .documents(documents)
                .build();

        webTestClient.post()
                .uri("/api/v1/indices/{indexName}/_bulk", testIndexName)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BulkResultResponse.class)
                .value(response -> {
                    assertThat(response.getTotalDocuments()).isEqualTo(3);
                    assertThat(response.getSuccessCount()).isEqualTo(3);
                    assertThat(response.hasFailures()).isFalse();
                });
    }

    @Test
    void shouldHandleEmptyBulkRequest() {
        BulkDocumentRequest request = BulkDocumentRequest.builder()
                .documents(List.of())
                .build();

        webTestClient.post()
                .uri("/api/v1/indices/{indexName}/_bulk", testIndexName)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn404ForBulkOnNonExistentIndex() {
        List<Map<String, Object>> documents = List.of(
                Map.of("id", "1", "name", "Test")
        );

        BulkDocumentRequest request = BulkDocumentRequest.builder()
                .documents(documents)
                .build();

        webTestClient.post()
                .uri("/api/v1/indices/{indexName}/_bulk", "non-existent-index")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }
}
