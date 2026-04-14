package com.opensearch.utility.command.document.domain.command;

import com.opensearch.utility.command.document.domain.Document;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchSaveCommand {

    private String targetIndex;
    private List<Document> documents;
    private String correlationId;
}
