package com.opensearch.utility.command.document.domain.dto.inbound;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDocumentRequest {

    @NotEmpty(message = "Documents list cannot be empty")
    private List<Map<String, Object>> documents;
}
