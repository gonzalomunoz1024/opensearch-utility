package com.opensearch.utility.command.index.domain.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReindexCommand {

    private String sourceIndex;
    private String destinationIndex;
    private Map<String, Object> query;
    private String script;
    private Map<String, Object> scriptParams;
    private boolean refresh;
    private int batchSize;
    private String conflicts;
}
