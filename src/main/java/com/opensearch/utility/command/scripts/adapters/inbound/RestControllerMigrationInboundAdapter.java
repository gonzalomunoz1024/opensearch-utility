package com.opensearch.utility.command.scripts.adapters.inbound;

import com.opensearch.utility.command.scripts.domain.command.StartMigrationCommand;
import com.opensearch.utility.command.scripts.domain.dto.inbound.StartMigrationRequest;
import com.opensearch.utility.command.scripts.domain.dto.outbound.MigrationProgressResponse;
import com.opensearch.utility.command.scripts.domain.dto.outbound.MigrationResponse;
import com.opensearch.utility.command.scripts.usecases.MigrationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/migration")
@RequiredArgsConstructor
public class RestControllerMigrationInboundAdapter {

    private final MigrationUseCase migrationUseCase;

    @PostMapping
    public Mono<ResponseEntity<MigrationResponse>> startMigration(
            @Valid @RequestBody StartMigrationRequest request) {
        log.info("REST: Starting migration from {} to {}, dryRun={}",
                request.getSource().getUrl(),
                request.getTarget().getUrl(),
                request.isDryRun());

        StartMigrationCommand command = buildCommand(request, false);

        return migrationUseCase.execute(command)
                .map(MigrationResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
    }

    @PostMapping("/dry-run")
    public Mono<ResponseEntity<MigrationResponse>> dryRunMigration(
            @Valid @RequestBody StartMigrationRequest request) {
        log.info("REST: Starting migration dry-run from {} to {}",
                request.getSource().getUrl(),
                request.getTarget().getUrl());

        StartMigrationCommand command = buildCommand(request, true);

        return migrationUseCase.execute(command)
                .map(MigrationResponse::from)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{migrationId}")
    public Mono<ResponseEntity<MigrationProgressResponse>> getMigrationStatus(
            @PathVariable String migrationId) {
        log.debug("REST: Get migration status for: {}", migrationId);

        return migrationUseCase.getMigrationStatus(migrationId)
                .map(MigrationProgressResponse::from)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    public Mono<ResponseEntity<MigrationProgressResponse>> getLatestMigration() {
        log.debug("REST: Get latest migration status");

        return migrationUseCase.getLatestMigration()
                .map(MigrationProgressResponse::from)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private StartMigrationCommand buildCommand(StartMigrationRequest request, boolean forceDryRun) {
        StartMigrationCommand.ClusterConfig sourceConfig = StartMigrationCommand.ClusterConfig.builder()
                .url(request.getSource().getUrl())
                .username(request.getSource().getUsername())
                .password(request.getSource().getPassword())
                .connectionTimeoutMs(request.getSource().getConnectionTimeoutMs())
                .socketTimeoutMs(request.getSource().getSocketTimeoutMs())
                .sslEnabled(request.getSource().isSslEnabled())
                .sslVerify(request.getSource().isSslVerify())
                .build();

        StartMigrationCommand.ClusterConfig targetConfig = StartMigrationCommand.ClusterConfig.builder()
                .url(request.getTarget().getUrl())
                .username(request.getTarget().getUsername())
                .password(request.getTarget().getPassword())
                .connectionTimeoutMs(request.getTarget().getConnectionTimeoutMs())
                .socketTimeoutMs(request.getTarget().getSocketTimeoutMs())
                .sslEnabled(request.getTarget().isSslEnabled())
                .sslVerify(request.getTarget().isSslVerify())
                .build();

        StartMigrationCommand.MigrationOptions options = null;
        if (request.getOptions() != null) {
            options = StartMigrationCommand.MigrationOptions.builder()
                    .batchSize(request.getOptions().getBatchSize())
                    .scrollTimeout(request.getOptions().getScrollTimeout())
                    .scrollSize(request.getOptions().getScrollSize())
                    .maxConcurrentIndices(request.getOptions().getMaxConcurrentIndices())
                    .continueOnFailure(request.getOptions().isContinueOnFailure())
                    .savedObjectsIndex(request.getOptions().getSavedObjectsIndex())
                    .savedObjectsTypes(request.getOptions().getSavedObjectsTypes())
                    .build();
        }

        return StartMigrationCommand.builder()
                .source(sourceConfig)
                .target(targetConfig)
                .dryRun(forceDryRun || request.isDryRun())
                .includeIndices(request.getIncludeIndices())
                .excludeIndices(request.getExcludeIndices())
                .migrateSavedObjects(request.isMigrateSavedObjects())
                .options(options)
                .build();
    }
}
