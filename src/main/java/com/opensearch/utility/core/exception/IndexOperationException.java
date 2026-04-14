package com.opensearch.utility.core.exception;

public class IndexOperationException extends OpenSearchUtilityException {

    public IndexOperationException(String message) {
        super(message, "INDEX_OPERATION_ERROR");
    }

    public IndexOperationException(String message, Throwable cause) {
        super(message, "INDEX_OPERATION_ERROR", cause);
    }

    public IndexOperationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public static class IndexNotFoundException extends IndexOperationException {
        public IndexNotFoundException(String indexName) {
            super("Index not found: " + indexName, "INDEX_NOT_FOUND");
        }
    }

    public static class IndexAlreadyExistsException extends IndexOperationException {
        public IndexAlreadyExistsException(String indexName) {
            super("Index already exists: " + indexName, "INDEX_ALREADY_EXISTS");
        }
    }

    public static class IndexCreationFailedException extends IndexOperationException {
        public IndexCreationFailedException(String indexName, Throwable cause) {
            super("Failed to create index: " + indexName, cause);
        }
    }
}
