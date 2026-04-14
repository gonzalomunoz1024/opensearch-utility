package com.opensearch.utility.command.index.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Index {

    private String name;
    private String uuid;
    private IndexSettings settings;
    private IndexHealth health;
    private long docsCount;
    private long storeSizeBytes;

    public enum IndexHealth {
        GREEN, YELLOW, RED
    }
}
