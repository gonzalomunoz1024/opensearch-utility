package com.opensearch.utility.command.index.domain.dto.inbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opensearch.utility.command.index.domain.IndexSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CreateIndexRequest {

    @NotBlank(message = "Index name is required")
    @Pattern(regexp = "^[a-z][a-z0-9_\\-]*$", message = "Index name must start with lowercase letter and contain only lowercase letters, numbers, underscores, and hyphens")
    private String indexName;

    private IndexSettings settings;

    private Map<String, Object> mappings;
}
