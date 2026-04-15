package com.opensearch.utility.command.kafka.transformer;

import com.opensearch.utility.command.kafka.domain.dto.StandardizedDocument;
import com.opensearch.utility.command.kafka.ports.outbound.EventTransformerPort;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
public abstract class AbstractEventTransformer implements EventTransformerPort {

    private final String supportedTopic;

    protected AbstractEventTransformer(String supportedTopic) {
        this.supportedTopic = supportedTopic;
    }

    @Override
    public boolean supports(String topic) {
        return supportedTopic.equals(topic);
    }

    @Override
    public String getSupportedTopic() {
        return supportedTopic;
    }

    @Override
    public Mono<StandardizedDocument> transform(Map<String, Object> rawEvent, String topic) {
        return Mono.fromCallable(() -> {
            log.debug("[KAFKA] Transforming event from topic: {}", topic);

            return StandardizedDocument.builder()
                    .rawEvent(rawEvent)
                    .apiVersion(extractApiVersion(rawEvent))
                    .kind(extractKind(rawEvent))
                    .source(extractSource(rawEvent))
                    .correlationId(extractCorrelationId(rawEvent))
                    .status(extractStatus(rawEvent))
                    .environment(extractEnvironment(rawEvent))
                    .cluster(extractCluster(rawEvent))
                    .lob(extractLob(rawEvent))
                    .applicationId(extractApplicationId(rawEvent))
                    .workflowName(extractWorkflowName(rawEvent))
                    .build();
        }).doOnError(e -> log.error("[KAFKA] Transformation failed for topic {}: {}", topic, e.getMessage()));
    }

    protected abstract String extractApiVersion(Map<String, Object> event);
    protected abstract String extractKind(Map<String, Object> event);
    protected abstract String extractSource(Map<String, Object> event);
    protected abstract String extractCorrelationId(Map<String, Object> event);
    protected abstract String extractStatus(Map<String, Object> event);
    protected abstract String extractEnvironment(Map<String, Object> event);
    protected abstract String extractCluster(Map<String, Object> event);
    protected abstract String extractLob(Map<String, Object> event);
    protected abstract String extractApplicationId(Map<String, Object> event);
    protected abstract String extractWorkflowName(Map<String, Object> event);

    @SuppressWarnings("unchecked")
    protected String extractString(Map<String, Object> map, String... path) {
        Object current = map;
        for (String key : path) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                return null;
            }
        }
        return current != null ? current.toString() : null;
    }

    protected String extractStringOrDefault(Map<String, Object> map, String defaultValue, String... path) {
        String value = extractString(map, path);
        return value != null ? value : defaultValue;
    }
}
