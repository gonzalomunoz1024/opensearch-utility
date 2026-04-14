package com.opensearch.utility.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class RetryableEvent extends BaseEvent {

    private int reprocessCount;

    public static final int DEFAULT_REPROCESS_COUNT = 3;

    public boolean canRetry() {
        return reprocessCount > 0;
    }

    public int decrementAndGetReprocessCount() {
        return --reprocessCount;
    }
}
