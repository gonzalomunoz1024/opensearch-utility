package com.opensearch.utility.command.scripts.domain.dto.outbound;

import com.opensearch.utility.command.scripts.domain.Migration;
import com.opensearch.utility.command.scripts.domain.MigrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationProgressResponse {

    private String migrationId;
    private MigrationStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int totalIndices;
    private int completedIndices;
    private int failedIndices;
    private long totalDocuments;
    private long migratedDocuments;
    private double progressPercentage;
    private String errorMessage;

    public static MigrationProgressResponse from(Migration migration) {
        double progress = 0.0;
        if (migration.getTotalIndices() > 0) {
            progress = ((double) (migration.getCompletedIndices() + migration.getFailedIndices())
                    / migration.getTotalIndices()) * 100;
        }

        return MigrationProgressResponse.builder()
                .migrationId(migration.getId())
                .status(migration.getStatus())
                .startedAt(migration.getStartedAt())
                .completedAt(migration.getCompletedAt())
                .totalIndices(migration.getTotalIndices())
                .completedIndices(migration.getCompletedIndices())
                .failedIndices(migration.getFailedIndices())
                .totalDocuments(migration.getTotalDocuments())
                .migratedDocuments(migration.getMigratedDocuments())
                .progressPercentage(progress)
                .errorMessage(migration.getErrorMessage())
                .build();
    }
}
