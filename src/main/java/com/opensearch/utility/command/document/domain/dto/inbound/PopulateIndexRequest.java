package com.opensearch.utility.command.document.domain.dto.inbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PopulateIndexRequest {

    @NotBlank(message = "Source endpoint is required")
    private String sourceEndpoint;
}
