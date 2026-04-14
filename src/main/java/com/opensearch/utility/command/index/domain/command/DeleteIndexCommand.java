package com.opensearch.utility.command.index.domain.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteIndexCommand {

    private String indexName;
}
