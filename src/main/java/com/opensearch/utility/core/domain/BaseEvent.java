package com.opensearch.utility.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    private String eventId;
    private Instant timestamp;
    private String correlationId;

    public static abstract class BaseEventBuilder<C extends BaseEvent, B extends BaseEventBuilder<C, B>> {
        public B withDefaults() {
            this.eventId = UUID.randomUUID().toString();
            this.timestamp = Instant.now();
            this.correlationId = UUID.randomUUID().toString();
            return self();
        }
    }
}
