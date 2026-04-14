package com.opensearch.utility.core.http;

import com.opensearch.utility.core.exception.ClusterOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSearchHttpClient {

    private final WebClient openSearchWebClient;

    public <T> Mono<T> get(String path, Class<T> responseType) {
        return execute(HttpMethod.GET, path, null, responseType);
    }

    public <T> Mono<T> get(String path, Map<String, String> queryParams, Class<T> responseType) {
        return openSearchWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path);
                    if (queryParams != null) {
                        queryParams.forEach(uriBuilder::queryParam);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(responseType)
                .doOnSubscribe(s -> log.debug("GET {}", path))
                .doOnSuccess(r -> log.debug("GET {} completed", path))
                .doOnError(e -> log.error("GET {} failed: {}", path, e.getMessage()));
    }

    public <T> Mono<T> post(String path, Object body, Class<T> responseType) {
        return execute(HttpMethod.POST, path, body, responseType);
    }

    public <T> Mono<T> put(String path, Object body, Class<T> responseType) {
        return execute(HttpMethod.PUT, path, body, responseType);
    }

    public Mono<Void> delete(String path) {
        return openSearchWebClient.delete()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(Void.class)
                .doOnSubscribe(s -> log.debug("DELETE {}", path))
                .doOnSuccess(r -> log.debug("DELETE {} completed", path))
                .doOnError(e -> log.error("DELETE {} failed: {}", path, e.getMessage()));
    }

    public Mono<Boolean> head(String path) {
        return openSearchWebClient.head()
                .uri(path)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.just(true);
                    } else if (response.statusCode().value() == 404) {
                        return Mono.just(false);
                    }
                    return handleError(response).then(Mono.just(false));
                })
                .doOnSubscribe(s -> log.debug("HEAD {}", path));
    }

    private <T> Mono<T> execute(HttpMethod method, String path, Object body, Class<T> responseType) {
        WebClient.RequestBodySpec requestSpec = openSearchWebClient
                .method(method)
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON);

        WebClient.RequestHeadersSpec<?> headersSpec = body != null
                ? requestSpec.bodyValue(body)
                : requestSpec;

        return headersSpec
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(responseType)
                .doOnSubscribe(s -> log.debug("{} {}", method, path))
                .doOnSuccess(r -> log.debug("{} {} completed", method, path))
                .doOnError(e -> log.error("{} {} failed: {}", method, path, e.getMessage()));
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("No error body")
                .flatMap(errorBody -> {
                    log.error("OpenSearch error response [{}]: {}", response.statusCode(), errorBody);
                    return Mono.error(new ClusterOperationException.ClusterUnreachableException(
                            "OpenSearch request failed with status " + response.statusCode() + ": " + errorBody));
                });
    }
}
