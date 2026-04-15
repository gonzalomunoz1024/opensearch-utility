package com.opensearch.utility.command.kafka.domain.event;

import com.opensearch.utility.core.domain.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KafkaEventTransformFailedEvent extends BaseEvent {

    private String sourceTopic;
    private Map<String, Object> rawEvent;
    private String failureReason;
    private String exceptionClass;
    private Integer kafkaPartition;
    private Long kafkaOffset;
}
