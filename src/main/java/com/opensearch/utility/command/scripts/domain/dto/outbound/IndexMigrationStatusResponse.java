package com.opensearch.utility.command.scripts.domain.dto.outbound;

import com.opensearch.utility.command.scripts.domain.IndexMigrationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexMigrationStatusResponse {

    private String indexName;
    private boolean success;
    private long documentCount;
    private long migratedCount;
    private long failedCount;
    private long durationMs;
    private String errorMessage;

    public static IndexMigrationStatusResponse from(IndexMigrationResult result) {
        return IndexMigrationStatusResponse.builder()
                .indexName(result.getIndexName())
                .success(result.isSuccess())
                .documentCount(result.getDocumentCount())
                .migratedCount(result.getMigratedCount())
                .failedCount(result.getFailedCount())
                .durationMs(result.getDurationMs())
                .errorMessage(result.getErrorMessage())
                .build();
    }
}
