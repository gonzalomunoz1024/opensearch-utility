package com.opensearch.utility.command.index.domain.dto.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReindexResponse {

    private long took;
    private boolean timedOut;
    private long total;
    private long created;
    private long updated;
    private long deleted;
    private int batches;
    private long versionConflicts;
    private long noops;
    private RetryStats retries;
    private List<String> failures;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryStats {
        private int bulk;
        private int search;
    }
}
