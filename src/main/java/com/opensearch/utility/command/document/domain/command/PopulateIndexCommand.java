package com.opensearch.utility.command.document.domain.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PopulateIndexCommand {

    private String targetIndex;
    private String sourceEndpoint;
    private String correlationId;
}
