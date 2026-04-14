package com.opensearch.utility.command.document.domain.event;

import com.opensearch.utility.core.domain.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DocumentBatchFailedEvent extends BaseEvent {

    private String targetIndex;
    private List<String> failedDocumentIds;
    private String failureReason;
    private int remainingRetries;
}
