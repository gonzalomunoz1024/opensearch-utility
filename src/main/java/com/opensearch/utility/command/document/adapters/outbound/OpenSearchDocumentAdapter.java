package com.opensearch.utility.command.document.adapters.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearch.utility.command.document.domain.BulkOperationResult;
import com.opensearch.utility.command.document.domain.Document;
import com.opensearch.utility.command.document.ports.outbound.DocumentPersistencePort;
import com.opensearch.utility.core.http.OpenSearchHttpClient;
import com.opensearch.utility.core.util.IdGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenSearchDocumentAdapter implements DocumentPersistencePort {

    private final OpenSearchHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;

    public OpenSearchDocumentAdapter(OpenSearchHttpClient httpClient,
                                      ObjectMapper objectMapper,
                                      IdGenerator idGenerator) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public Mono<BulkOperationResult> bulkSave(String indexName, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return Mono.just(BulkOperationResult.builder()
                    .took(0)
                    .errors(false)
                    .items(List.of())
                    .build());
        }

        String bulkBody = buildBulkRequestBody(indexName, documents);

        return httpClient.post("/_bulk", bulkBody, BulkResponse.class)
                .map(response -> mapToBulkOperationResult(response, documents))
                .doOnSuccess(result -> log.debug("Bulk operation completed: {} docs, errors: {}",
                        result.getItems().size(), result.isErrors()))
                .doOnError(e -> log.error("Bulk operation failed: {}", e.getMessage()));
    }

    @Override
    public Mono<Document> save(String indexName, Document document) {
        String docId = document.getId() != null ? document.getId() : idGenerator.generate();
        String path = "/" + indexName + "/_doc/" + docId;

        return httpClient.put(path, document.getSource(), IndexResponse.class)
                .map(response -> Document.builder()
                        .id(response.getId())
                        .index(response.getIndex())
                        .version(response.getVersion())
                        .source(document.getSource())
                        .build());
    }

    @Override
    public Mono<Document> getById(String indexName, String documentId) {
        String path = "/" + indexName + "/_doc/" + documentId;

        return httpClient.get(path, GetResponse.class)
                .map(response -> Document.builder()
                        .id(response.getId())
                        .index(response.getIndex())
                        .version(response.getVersion())
                        .source(response.getSource())
                        .seqNo(response.getSeqNo())
                        .primaryTerm(response.getPrimaryTerm())
                        .build());
    }

    @Override
    public Mono<Void> delete(String indexName, String documentId) {
        String path = "/" + indexName + "/_doc/" + documentId;
        return httpClient.delete(path);
    }

    private String buildBulkRequestBody(String indexName, List<Document> documents) {
        StringBuilder builder = new StringBuilder();
        for (Document doc : documents) {
            String docId = doc.getId() != null ? doc.getId() : idGenerator.generate();

            // Action line
            builder.append("{\"index\":{\"_index\":\"")
                    .append(indexName)
                    .append("\",\"_id\":\"")
                    .append(docId)
                    .append("\"}}\n");

            // Document source line
            builder.append(toJson(doc.getSource())).append("\n");
        }
        return builder.toString();
    }

    private String toJson(Map<String, Object> source) {
        if (source == null) return "{}";
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize document source: {}", e.getMessage());
            return "{}";
        }
    }

    private BulkOperationResult mapToBulkOperationResult(BulkResponse response, List<Document> documents) {
        List<BulkOperationResult.BulkItemResult> items = new ArrayList<>();

        if (response.getItems() != null) {
            for (int i = 0; i < response.getItems().size(); i++) {
                Map<String, Object> item = response.getItems().get(i);
                Map<String, Object> indexResult = (Map<String, Object>) item.get("index");

                if (indexResult != null) {
                    String errorType = null;
                    String errorReason = null;
                    Map<String, Object> error = (Map<String, Object>) indexResult.get("error");
                    if (error != null) {
                        errorType = (String) error.get("type");
                        errorReason = (String) error.get("reason");
                    }

                    Integer status = (Integer) indexResult.get("status");
                    boolean success = status != null && status >= 200 && status < 300;

                    items.add(BulkOperationResult.BulkItemResult.builder()
                            .id((String) indexResult.get("_id"))
                            .index((String) indexResult.get("_index"))
                            .result((String) indexResult.get("result"))
                            .status(status != null ? status : 0)
                            .success(success)
                            .errorType(errorType)
                            .errorReason(errorReason)
                            .build());
                }
            }
        }

        return BulkOperationResult.builder()
                .took(response.getTook())
                .errors(response.isErrors())
                .items(items)
                .build();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BulkResponse {
        private long took;
        private boolean errors;
        private List<Map<String, Object>> items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IndexResponse {
        @JsonProperty("_index")
        private String index;
        @JsonProperty("_id")
        private String id;
        @JsonProperty("_version")
        private Long version;
        private String result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GetResponse {
        @JsonProperty("_index")
        private String index;
        @JsonProperty("_id")
        private String id;
        @JsonProperty("_version")
        private Long version;
        @JsonProperty("_seq_no")
        private Long seqNo;
        @JsonProperty("_primary_term")
        private Long primaryTerm;
        @JsonProperty("_source")
        private Map<String, Object> source;
        private boolean found;
    }
}
