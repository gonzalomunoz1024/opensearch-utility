package com.opensearch.utility.command.index.domain.dto.inbound;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReindexRequest {

    @NotBlank(message = "Source index is required")
    private String sourceIndex;

    @NotBlank(message = "Destination index is required")
    private String destinationIndex;

    private Map<String, Object> query;

    private String script;

    private Map<String, Object> scriptParams;

    @Builder.Default
    private boolean refresh = true;

    @Builder.Default
    private int batchSize = 1000;

    @Builder.Default
    private String conflicts = "proceed";
}
