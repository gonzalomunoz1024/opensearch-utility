package com.opensearch.utility.core.exception;

public class DocumentOperationException extends OpenSearchUtilityException {

    public DocumentOperationException(String message) {
        super(message, "DOCUMENT_OPERATION_ERROR");
    }

    public DocumentOperationException(String message, Throwable cause) {
        super(message, "DOCUMENT_OPERATION_ERROR", cause);
    }

    public DocumentOperationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public static class BulkOperationFailedException extends DocumentOperationException {
        public BulkOperationFailedException(String message) {
            super(message, "BULK_OPERATION_FAILED");
        }

        public BulkOperationFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DocumentNotFoundException extends DocumentOperationException {
        public DocumentNotFoundException(String indexName, String documentId) {
            super("Document not found: " + indexName + "/" + documentId, "DOCUMENT_NOT_FOUND");
        }
    }

    public static class PopulateFailedException extends DocumentOperationException {
        public PopulateFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
