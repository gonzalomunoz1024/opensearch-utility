package com.opensearch.utility.command.document.domain.dto.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PopulateStatusResponse {

    private String jobId;
    private String targetIndex;
    private String sourceEndpoint;
    private String status;
    private long documentsProcessed;
    private long documentsSucceeded;
    private long documentsFailed;
    private Instant startedAt;
    private Instant completedAt;
}
