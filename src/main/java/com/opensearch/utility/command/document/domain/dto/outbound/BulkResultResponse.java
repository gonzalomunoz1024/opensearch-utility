package com.opensearch.utility.command.document.domain.dto.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opensearch.utility.command.document.domain.BulkOperationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkResultResponse {

    private long took;
    private boolean errors;
    private int totalDocuments;
    private int successfulDocuments;
    private int failedDocuments;
    private List<FailedItem> failedItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItem {
        private String id;
        private String index;
        private String errorType;
        private String errorReason;
    }

    public static BulkResultResponse from(BulkOperationResult result) {
        List<FailedItem> failedItems = result.getFailedItems().stream()
                .map(item -> FailedItem.builder()
                        .id(item.getId())
                        .index(item.getIndex())
                        .errorType(item.getErrorType())
                        .errorReason(item.getErrorReason())
                        .build())
                .toList();

        int totalDocs = result.getItems() != null ? result.getItems().size() : 0;
        int failedDocs = failedItems.size();

        return BulkResultResponse.builder()
                .took(result.getTook())
                .errors(result.isErrors())
                .totalDocuments(totalDocs)
                .successfulDocuments(totalDocs - failedDocs)
                .failedDocuments(failedDocs)
                .failedItems(failedItems)
                .build();
    }
}
