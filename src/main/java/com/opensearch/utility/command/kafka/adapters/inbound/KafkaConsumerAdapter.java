package com.opensearch.utility.command.kafka.adapters.inbound;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearch.utility.command.document.domain.Document;
import com.opensearch.utility.command.document.domain.event.DocumentBatchReceivedEvent;
import com.opensearch.utility.command.kafka.domain.dto.StandardizedDocument;
import com.opensearch.utility.command.kafka.domain.event.KafkaEventTransformFailedEvent;
import com.opensearch.utility.command.kafka.transformer.EventTransformerRegistry;
import com.opensearch.utility.core.config.BatchConfig;
import com.opensearch.utility.core.domain.RetryableEvent;
import com.opensearch.utility.core.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class KafkaConsumerAdapter {

    private final EventTransformerRegistry transformerRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;
    private final BatchConfig batchConfig;

    public KafkaConsumerAdapter(
            EventTransformerRegistry transformerRegistry,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            IdGenerator idGenerator,
            BatchConfig batchConfig) {
        this.transformerRegistry = transformerRegistry;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.batchConfig = batchConfig;
    }

    private Function<Flux<Message<byte[]>>, Mono<Void>> createTopicConsumer(String topicName) {
        return messages -> messages
                .bufferTimeout(batchConfig.getSize(), Duration.ofSeconds(batchConfig.getTimeoutSeconds()))
                .flatMap(batch -> processBatch(batch, topicName))
                .doOnError(e -> log.error("[KAFKA] Error processing batch from {}: {}", topicName, e.getMessage()))
                .then();
    }

    private Mono<Void> processBatch(List<Message<byte[]>> messages, String topicName) {
        String correlationId = idGenerator.generate();
        log.info("[KAFKA] Processing batch of {} messages from topic: {}", messages.size(), topicName);

        return Flux.fromIterable(messages)
                .flatMap(msg -> transformMessage(msg, topicName))
                .collectList()
                .flatMap(standardizedDocs -> {
                    if (standardizedDocs.isEmpty()) {
                        log.warn("[KAFKA] No valid documents after transformation from topic: {}", topicName);
                        return Mono.empty();
                    }

                    Map<String, List<StandardizedDocument>> byIndex = standardizedDocs.stream()
                            .collect(Collectors.groupingBy(StandardizedDocument::getTargetIndex));

                    byIndex.forEach((indexName, docs) -> {
                        List<Document> documents = docs.stream()
                                .map(this::toDocument)
                                .toList();

                        DocumentBatchReceivedEvent event = DocumentBatchReceivedEvent.builder()
                                .withDefaults()
                                .correlationId(correlationId)
                                .targetIndex(indexName)
                                .documents(documents)
                                .sourceEndpoint("kafka:" + topicName)
                                .reprocessCount(RetryableEvent.DEFAULT_REPROCESS_COUNT)
                                .build();

                        eventPublisher.publishEvent(event);
                        log.debug("[KAFKA] Published batch event: {} docs for index {}",
                                documents.size(), indexName);
                    });

                    return Mono.empty();
                });
    }

    private Mono<StandardizedDocument> transformMessage(Message<byte[]> message, String topicName) {
        return Mono.fromCallable(() -> {
                    Map<String, Object> rawEvent = objectMapper.readValue(
                            message.getPayload(),
                            new TypeReference<Map<String, Object>>() {}
                    );
                    return rawEvent;
                })
                .flatMap(rawEvent -> transformerRegistry.getTransformerOrThrow(topicName)
                        .transform(rawEvent, topicName))
                .onErrorResume(e -> {
                    log.error("[KAFKA] Failed to transform message from {}: {}", topicName, e.getMessage());
                    publishTransformFailedEvent(message, topicName, e);
                    return Mono.empty();
                });
    }

    private void publishTransformFailedEvent(Message<byte[]> message, String topicName, Throwable error) {
        try {
            Map<String, Object> rawEvent = objectMapper.readValue(
                    message.getPayload(),
                    new TypeReference<Map<String, Object>>() {}
            );

            KafkaEventTransformFailedEvent event = KafkaEventTransformFailedEvent.builder()
                    .withDefaults()
                    .sourceTopic(topicName)
                    .rawEvent(rawEvent)
                    .failureReason(error.getMessage())
                    .exceptionClass(error.getClass().getName())
                    .build();

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to publish transform failed event: {}", e.getMessage());
        }
    }

    private Document toDocument(StandardizedDocument standardizedDoc) {
        return Document.builder()
                .id(idGenerator.extractOrGenerate(standardizedDoc.getRawEvent()))
                .source(standardizedDoc.toSourceMap())
                .index(standardizedDoc.getTargetIndex())
                .build();
    }

    @Bean
    public Function<Flux<Message<byte[]>>, Mono<Void>> topicAConsumer() {
        return createTopicConsumer("topic-a");
    }

    @Bean
    public Function<Flux<Message<byte[]>>, Mono<Void>> topicBConsumer() {
        return createTopicConsumer("topic-b");
    }
}
