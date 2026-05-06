package com.opensearch.utility.command.scripts.domain.dto.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartMigrationRequest {

    @NotNull(message = "Source cluster configuration is required")
    @Valid
    private ClusterConfig source;

    @NotNull(message = "Target cluster configuration is required")
    @Valid
    private ClusterConfig target;

    private boolean dryRun;
    private List<String> includeIndices;
    private List<String> excludeIndices;
    @Builder.Default
    private boolean migrateSavedObjects = true;
    private MigrationOptionsRequest options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterConfig {
        @NotNull(message = "URL is required")
        private String url;
        @Builder.Default
        private String username = "admin";
        @Builder.Default
        private String password = "admin";
        @Builder.Default
        private int connectionTimeoutMs = 5000;
        @Builder.Default
        private int socketTimeoutMs = 120000;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MigrationOptionsRequest {
        @Builder.Default
        private int batchSize = 1000;
        @Builder.Default
        private String scrollTimeout = "5m";
        @Builder.Default
        private int scrollSize = 1000;
        @Builder.Default
        private int maxConcurrentIndices = 3;
        @Builder.Default
        private boolean continueOnFailure = true;
        @Builder.Default
        private String savedObjectsIndex = ".opensearch_dashboards";
        @Builder.Default
        private List<String> savedObjectsTypes = List.of("visualization", "dashboard", "search", "index-pattern");
    }
}
