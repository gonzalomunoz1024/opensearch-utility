package com.opensearch.utility.command.document.domain.event;

import com.opensearch.utility.command.document.domain.Document;
import com.opensearch.utility.core.domain.RetryableEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DocumentBatchReceivedEvent extends RetryableEvent {

    private String targetIndex;
    private List<Document> documents;
    private String sourceEndpoint;

    public int getBatchSize() {
        return documents != null ? documents.size() : 0;
    }
}
