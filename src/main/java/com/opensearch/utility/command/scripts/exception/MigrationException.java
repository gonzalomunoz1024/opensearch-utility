package com.opensearch.utility.command.scripts.exception;

import com.opensearch.utility.core.exception.OpenSearchUtilityException;
import lombok.Getter;

@Getter
public class MigrationException extends OpenSearchUtilityException {

    public MigrationException(String message) {
        super(message, "MIGRATION_ERROR");
    }

    public MigrationException(String message, Throwable cause) {
        super(message, "MIGRATION_ERROR", cause);
    }

    public static class SourceClusterUnreachableException extends MigrationException {
        public SourceClusterUnreachableException(String message) {
            super("Source cluster unreachable: " + message);
        }

        public SourceClusterUnreachableException(String message, Throwable cause) {
            super("Source cluster unreachable: " + message, cause);
        }
    }

    public static class TargetClusterUnreachableException extends MigrationException {
        public TargetClusterUnreachableException(String message) {
            super("Target cluster unreachable: " + message);
        }

        public TargetClusterUnreachableException(String message, Throwable cause) {
            super("Target cluster unreachable: " + message, cause);
        }
    }

    public static class IndexMigrationException extends MigrationException {
        private final String indexName;

        public IndexMigrationException(String indexName, String message) {
            super("Failed to migrate index '" + indexName + "': " + message);
            this.indexName = indexName;
        }

        public IndexMigrationException(String indexName, String message, Throwable cause) {
            super("Failed to migrate index '" + indexName + "': " + message, cause);
            this.indexName = indexName;
        }

        public String getIndexName() {
            return indexName;
        }
    }

    public static class SavedObjectsMigrationException extends MigrationException {
        public SavedObjectsMigrationException(String message) {
            super("Failed to migrate saved objects: " + message);
        }

        public SavedObjectsMigrationException(String message, Throwable cause) {
            super("Failed to migrate saved objects: " + message, cause);
        }
    }
}
