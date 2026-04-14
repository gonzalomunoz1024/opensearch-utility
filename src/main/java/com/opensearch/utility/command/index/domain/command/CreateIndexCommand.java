package com.opensearch.utility.command.index.domain.command;

import com.opensearch.utility.command.index.domain.IndexSettings;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CreateIndexCommand {

    private String indexName;
    private IndexSettings settings;
    private Map<String, Object> mappings;
}
