package com.opensearch.utility.command.scripts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexMigrationResult {

    private String indexName;
    private boolean success;
    private long documentCount;
    private long migratedCount;
    private long failedCount;
    private long durationMs;
    private String errorMessage;
    @Builder.Default
    private List<String> failedDocumentIds = new ArrayList<>();

    public static IndexMigrationResult success(String indexName, long documentCount, long migratedCount, long durationMs) {
        return IndexMigrationResult.builder()
                .indexName(indexName)
                .success(true)
                .documentCount(documentCount)
                .migratedCount(migratedCount)
                .failedCount(0)
                .durationMs(durationMs)
                .build();
    }

    public static IndexMigrationResult failure(String indexName, String errorMessage) {
        return IndexMigrationResult.builder()
                .indexName(indexName)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
