package com.opensearch.utility.command.scripts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Migration {

    private String id;
    private MigrationStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int totalIndices;
    private int completedIndices;
    private int failedIndices;
    private long totalDocuments;
    private long migratedDocuments;
    @Builder.Default
    private List<IndexMigrationResult> indexResults = new ArrayList<>();
    private String errorMessage;
    private boolean dryRun;

    public void addIndexResult(IndexMigrationResult result) {
        if (indexResults == null) {
            indexResults = new ArrayList<>();
        }
        indexResults.add(result);
        if (result.isSuccess()) {
            completedIndices++;
            migratedDocuments += result.getMigratedCount();
        } else {
            failedIndices++;
        }
    }
}
