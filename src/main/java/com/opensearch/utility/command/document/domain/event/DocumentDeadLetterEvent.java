package com.opensearch.utility.command.document.domain.event;

import com.opensearch.utility.command.document.domain.Document;
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
public class DocumentDeadLetterEvent extends BaseEvent {

    private String targetIndex;
    private List<Document> documents;
    private String sourceEndpoint;
    private String failureReason;
    private int originalReprocessCount;

    public List<String> getDocumentIds() {
        if (documents == null) return List.of();
        return documents.stream()
                .map(Document::getId)
                .toList();
    }

    public int getDocumentCount() {
        return documents != null ? documents.size() : 0;
    }
}
