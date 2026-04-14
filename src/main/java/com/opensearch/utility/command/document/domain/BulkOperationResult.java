package com.opensearch.utility.command.document.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResult {

    private long took;
    private boolean errors;
    private List<BulkItemResult> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkItemResult {
        private String id;
        private String index;
        private String result;
        private int status;
        private boolean success;
        private String errorType;
        private String errorReason;
    }

    public List<BulkItemResult> getFailedItems() {
        if (items == null) return List.of();
        return items.stream()
                .filter(item -> !item.isSuccess())
                .toList();
    }

    public List<String> getFailedDocumentIds() {
        return getFailedItems().stream()
                .map(BulkItemResult::getId)
                .toList();
    }

    public boolean hasFailures() {
        return errors;
    }
}
