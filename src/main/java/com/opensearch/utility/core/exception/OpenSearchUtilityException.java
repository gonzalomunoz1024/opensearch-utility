package com.opensearch.utility.core.exception;

import lombok.Getter;

@Getter
public class OpenSearchUtilityException extends RuntimeException {

    private final String errorCode;
    private final transient Object details;

    public OpenSearchUtilityException(String message) {
        super(message);
        this.errorCode = "OPENSEARCH_UTILITY_ERROR";
        this.details = null;
    }

    public OpenSearchUtilityException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "OPENSEARCH_UTILITY_ERROR";
        this.details = null;
    }

    public OpenSearchUtilityException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public OpenSearchUtilityException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public OpenSearchUtilityException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }
}
