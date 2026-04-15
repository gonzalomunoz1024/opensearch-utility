package com.opensearch.utility.command.kafka.transformer;

import com.opensearch.utility.command.kafka.ports.outbound.EventTransformerPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EventTransformerRegistry {

    private final Map<String, EventTransformerPort> transformersByTopic;

    public EventTransformerRegistry(List<EventTransformerPort> transformers) {
        this.transformersByTopic = transformers.stream()
                .collect(Collectors.toMap(
                        EventTransformerPort::getSupportedTopic,
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("[KAFKA] Duplicate transformer for topic, using: {}",
                                    replacement.getClass().getSimpleName());
                            return replacement;
                        }
                ));

        log.info("[KAFKA] Registered {} event transformers for topics: {}",
                transformersByTopic.size(), transformersByTopic.keySet());
    }

    public Optional<EventTransformerPort> getTransformer(String topic) {
        return Optional.ofNullable(transformersByTopic.get(topic));
    }

    public EventTransformerPort getTransformerOrThrow(String topic) {
        return getTransformer(topic)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No transformer registered for topic: " + topic));
    }

    public boolean hasTransformer(String topic) {
        return transformersByTopic.containsKey(topic);
    }
}
