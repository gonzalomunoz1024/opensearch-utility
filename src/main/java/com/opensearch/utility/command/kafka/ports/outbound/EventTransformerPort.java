package com.opensearch.utility.command.kafka.ports.outbound;

import com.opensearch.utility.command.kafka.domain.dto.StandardizedDocument;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EventTransformerPort {

    Mono<StandardizedDocument> transform(Map<String, Object> rawEvent, String topic);

    boolean supports(String topic);

    String getSupportedTopic();
}
