package com.opensearch.utility.command.scripts.domain.dto.outbound;

import com.opensearch.utility.command.scripts.domain.Migration;
import com.opensearch.utility.command.scripts.domain.MigrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationResponse {

    private String migrationId;
    private MigrationStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int totalIndices;
    private int completedIndices;
    private int failedIndices;
    private long totalDocuments;
    private long migratedDocuments;
    private List<IndexMigrationStatusResponse> indexResults;
    private String errorMessage;
    private boolean dryRun;

    public static MigrationResponse from(Migration migration) {
        return MigrationResponse.builder()
                .migrationId(migration.getId())
                .status(migration.getStatus())
                .startedAt(migration.getStartedAt())
                .completedAt(migration.getCompletedAt())
                .totalIndices(migration.getTotalIndices())
                .completedIndices(migration.getCompletedIndices())
                .failedIndices(migration.getFailedIndices())
                .totalDocuments(migration.getTotalDocuments())
                .migratedDocuments(migration.getMigratedDocuments())
                .indexResults(migration.getIndexResults() != null
                        ? migration.getIndexResults().stream()
                                .map(IndexMigrationStatusResponse::from)
                                .collect(Collectors.toList())
                        : null)
                .errorMessage(migration.getErrorMessage())
                .dryRun(migration.isDryRun())
                .build();
    }
}
