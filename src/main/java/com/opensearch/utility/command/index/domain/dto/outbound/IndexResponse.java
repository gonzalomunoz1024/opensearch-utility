package com.opensearch.utility.command.index.domain.dto.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opensearch.utility.command.index.domain.Index;
import com.opensearch.utility.command.index.domain.IndexSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndexResponse {

    private String name;
    private String uuid;
    private String health;
    private IndexSettings settings;
    private long docsCount;
    private String storeSize;

    public static IndexResponse from(Index index) {
        return IndexResponse.builder()
                .name(index.getName())
                .uuid(index.getUuid())
                .health(index.getHealth() != null ? index.getHealth().name().toLowerCase() : null)
                .settings(index.getSettings())
                .docsCount(index.getDocsCount())
                .storeSize(formatBytes(index.getStoreSizeBytes()))
                .build();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }
}
