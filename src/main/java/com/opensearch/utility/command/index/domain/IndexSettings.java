package com.opensearch.utility.command.index.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndexSettings {

    @JsonProperty("number_of_shards")
    @Builder.Default
    private Integer numberOfShards = 1;

    @JsonProperty("number_of_replicas")
    @Builder.Default
    private Integer numberOfReplicas = 1;

    @JsonProperty("refresh_interval")
    private String refreshInterval;

    @JsonProperty("max_result_window")
    private Integer maxResultWindow;

    private Map<String, Object> analysis;
}
