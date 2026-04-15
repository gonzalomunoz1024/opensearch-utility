package com.opensearch.utility.command.kafka.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardizedDocument {

    private Map<String, Object> rawEvent;
    private String apiVersion;
    private String kind;
    private String source;
    private String correlationId;
    private String status;
    private String environment;
    private String cluster;
    private String lob;
    private String applicationId;
    private String workflowName;

    public String getTargetIndex() {
        return apiVersion;
    }

    public Map<String, Object> toSourceMap() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("raw_event", rawEvent);
        sourceMap.put("apiVersion", apiVersion);
        sourceMap.put("kind", kind);
        sourceMap.put("source", source);
        sourceMap.put("correlationId", correlationId);
        sourceMap.put("status", status);
        sourceMap.put("environment", environment);
        sourceMap.put("cluster", cluster);
        sourceMap.put("lob", lob);
        sourceMap.put("applicationId", applicationId);
        sourceMap.put("workflowName", workflowName);
        return sourceMap;
    }
}
