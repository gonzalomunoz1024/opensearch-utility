package com.opensearch.utility.command.document.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class Document {

    private String id;
    private Map<String, Object> source;
    private String index;
    private Long version;
    private Long seqNo;
    private Long primaryTerm;
}
