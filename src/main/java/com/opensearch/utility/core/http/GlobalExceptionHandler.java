package com.opensearch.utility.core.http;

import com.opensearch.utility.core.exception.ClusterOperationException;
import com.opensearch.utility.core.exception.DocumentOperationException;
import com.opensearch.utility.core.exception.IndexOperationException;
import com.opensearch.utility.core.exception.OpenSearchUtilityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IndexOperationException.IndexNotFoundException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleIndexNotFound(IndexOperationException.IndexNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(IndexOperationException.IndexAlreadyExistsException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleIndexAlreadyExists(IndexOperationException.IndexAlreadyExistsException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(DocumentOperationException.DocumentNotFoundException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleDocumentNotFound(DocumentOperationException.DocumentNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ClusterOperationException.ClusterUnreachableException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleClusterUnreachable(ClusterOperationException.ClusterUnreachableException ex) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex);
    }

    @ExceptionHandler(OpenSearchUtilityException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleOpenSearchUtilityException(OpenSearchUtilityException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "message", "An unexpected error occurred",
                        "errorCode", "INTERNAL_ERROR"
                )));
    }

    private Mono<ResponseEntity<Map<String, Object>>> buildErrorResponse(HttpStatus status, OpenSearchUtilityException ex) {
        log.error("Error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return Mono.just(ResponseEntity
                .status(status)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status.value(),
                        "error", status.getReasonPhrase(),
                        "message", ex.getMessage(),
                        "errorCode", ex.getErrorCode()
                )));
    }
}
