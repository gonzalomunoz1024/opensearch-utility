package com.opensearch.utility.command.document.adapters.inbound;

import com.opensearch.utility.command.document.domain.command.PopulateIndexCommand;
import com.opensearch.utility.command.document.domain.dto.inbound.PopulateIndexRequest;
import com.opensearch.utility.command.document.domain.dto.outbound.PopulateStatusResponse;
import com.opensearch.utility.command.document.usecases.PopulateIndexUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/indices/{indexName}")
@RequiredArgsConstructor
public class RestControllerDocumentInboundAdapter {

    private final PopulateIndexUseCase populateIndexUseCase;

    @PostMapping("/populate")
    public Mono<ResponseEntity<PopulateStatusResponse>> populateIndex(
            @PathVariable String indexName,
            @Valid @RequestBody PopulateIndexRequest request) {

        log.info("REST: Populate index {} from {}", indexName, request.getSourceEndpoint());

        PopulateIndexCommand command = PopulateIndexCommand.builder()
                .targetIndex(indexName)
                .sourceEndpoint(request.getSourceEndpoint())
                .correlationId(UUID.randomUUID().toString())
                .build();

        return populateIndexUseCase.execute(command)
                .map(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
    }
}
