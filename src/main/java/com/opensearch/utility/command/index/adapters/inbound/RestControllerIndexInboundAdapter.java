package com.opensearch.utility.command.index.adapters.inbound;

import com.opensearch.utility.command.index.domain.command.CreateIndexCommand;
import com.opensearch.utility.command.index.domain.command.DeleteIndexCommand;
import com.opensearch.utility.command.index.domain.command.ReindexCommand;
import com.opensearch.utility.command.index.domain.dto.inbound.CreateIndexRequest;
import com.opensearch.utility.command.index.domain.dto.inbound.ReindexRequest;
import com.opensearch.utility.command.index.domain.dto.outbound.IndexResponse;
import com.opensearch.utility.command.index.domain.dto.outbound.ReindexResponse;
import com.opensearch.utility.command.index.usecases.CreateIndexUseCase;
import com.opensearch.utility.command.index.usecases.DeleteIndexUseCase;
import com.opensearch.utility.command.index.usecases.IndexQueryUseCase;
import com.opensearch.utility.command.index.usecases.ReindexUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/indices")
@RequiredArgsConstructor
public class RestControllerIndexInboundAdapter {

    private final CreateIndexUseCase createIndexUseCase;
    private final DeleteIndexUseCase deleteIndexUseCase;
    private final IndexQueryUseCase indexQueryUseCase;
    private final ReindexUseCase reindexUseCase;

    @PostMapping
    public Mono<ResponseEntity<IndexResponse>> createIndex(@Valid @RequestBody CreateIndexRequest request) {
        log.info("REST: Create index request for: {}", request.getIndexName());

        CreateIndexCommand command = CreateIndexCommand.builder()
                .indexName(request.getIndexName())
                .settings(request.getSettings())
                .mappings(request.getMappings())
                .build();

        return createIndexUseCase.execute(command)
                .map(IndexResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping
    public Flux<IndexResponse> listIndices() {
        log.debug("REST: List indices request");
        return indexQueryUseCase.listIndices()
                .map(IndexResponse::from);
    }

    @GetMapping("/{indexName}")
    public Mono<ResponseEntity<IndexResponse>> getIndex(@PathVariable String indexName) {
        log.debug("REST: Get index request for: {}", indexName);
        return indexQueryUseCase.getIndex(indexName)
                .map(IndexResponse::from)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{indexName}")
    public Mono<ResponseEntity<Void>> deleteIndex(@PathVariable String indexName) {
        log.info("REST: Delete index request for: {}", indexName);

        DeleteIndexCommand command = DeleteIndexCommand.builder()
                .indexName(indexName)
                .build();

        return deleteIndexUseCase.execute(command)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @RequestMapping(value = "/{indexName}", method = RequestMethod.HEAD)
    public Mono<ResponseEntity<Void>> indexExists(@PathVariable String indexName) {
        return indexQueryUseCase.indexExists(indexName)
                .map(exists -> exists
                        ? ResponseEntity.ok().<Void>build()
                        : ResponseEntity.notFound().<Void>build());
    }

    @PostMapping("/_reindex")
    public Mono<ResponseEntity<ReindexResponse>> reindex(@Valid @RequestBody ReindexRequest request) {
        log.info("REST: Reindex request from {} to {}", request.getSourceIndex(), request.getDestinationIndex());

        ReindexCommand command = ReindexCommand.builder()
                .sourceIndex(request.getSourceIndex())
                .destinationIndex(request.getDestinationIndex())
                .query(request.getQuery())
                .script(request.getScript())
                .scriptParams(request.getScriptParams())
                .refresh(request.isRefresh())
                .batchSize(request.getBatchSize())
                .conflicts(request.getConflicts())
                .build();

        return reindexUseCase.execute(command)
                .map(ResponseEntity::ok);
    }
}
