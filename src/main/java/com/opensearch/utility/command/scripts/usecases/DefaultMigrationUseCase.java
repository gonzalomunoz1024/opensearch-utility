package com.opensearch.utility.command.scripts.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearch.utility.command.document.domain.BulkOperationResult;
import com.opensearch.utility.command.scripts.adapters.outbound.SourceOpenSearchAdapter;
import com.opensearch.utility.command.scripts.adapters.outbound.TargetOpenSearchAdapter;
import com.opensearch.utility.command.scripts.config.MigrationWebClientFactory;
import com.opensearch.utility.command.scripts.domain.IndexMigrationResult;
import com.opensearch.utility.command.scripts.domain.Migration;
import com.opensearch.utility.command.scripts.domain.MigrationStatus;
import com.opensearch.utility.command.scripts.domain.command.StartMigrationCommand;
import com.opensearch.utility.command.scripts.exception.MigrationException;
import com.opensearch.utility.command.scripts.ports.outbound.SourceClusterPort;
import com.opensearch.utility.command.scripts.ports.outbound.TargetClusterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMigrationUseCase implements MigrationUseCase {

    private final MigrationWebClientFactory webClientFactory;
    private final ObjectMapper objectMapper;

    private final Map<String, Migration> migrationStore = new ConcurrentHashMap<>();
    private final AtomicReference<String> latestMigrationId = new AtomicReference<>();

    @Override
    public Mono<Migration> execute(StartMigrationCommand command) {
        // Create WebClients for source and target
        WebClient sourceWebClient = webClientFactory.createWebClient(command.getSource());
        WebClient targetWebClient = webClientFactory.createWebClient(command.getTarget());

        // Create adapters
        SourceClusterPort sourceCluster = new SourceOpenSearchAdapter(sourceWebClient);
        TargetClusterPort targetCluster = new TargetOpenSearchAdapter(targetWebClient, objectMapper);

        Migration migration = createMigration(command);
        migrationStore.put(migration.getId(), migration);
        latestMigrationId.set(migration.getId());

        return validateClusters(sourceCluster, targetCluster)
                .then(discoverAndMigrateIndices(migration, command, sourceCluster, targetCluster))
                .then(migrateSavedObjects(migration, command, sourceCluster, targetCluster))
                .then(finalizeMigration(migration))
                .onErrorResume(e -> handleMigrationError(migration, e));
    }

    @Override
    public Mono<Migration> getMigrationStatus(String migrationId) {
        Migration migration = migrationStore.get(migrationId);
        return migration != null ? Mono.just(migration) : Mono.empty();
    }

    @Override
    public Mono<Migration> getLatestMigration() {
        String id = latestMigrationId.get();
        return id != null ? getMigrationStatus(id) : Mono.empty();
    }

    private Migration createMigration(StartMigrationCommand command) {
        return Migration.builder()
                .id(UUID.randomUUID().toString())
                .status(MigrationStatus.PENDING)
                .startedAt(LocalDateTime.now())
                .dryRun(command.isDryRun())
                .build();
    }

    private Mono<Void> validateClusters(SourceClusterPort sourceCluster, TargetClusterPort targetCluster) {
        return Mono.zip(
                sourceCluster.isReachable(),
                targetCluster.isReachable()
        ).flatMap(tuple -> {
            boolean sourceReachable = tuple.getT1();
            boolean targetReachable = tuple.getT2();

            if (!sourceReachable) {
                return Mono.error(new MigrationException.SourceClusterUnreachableException(
                        "Cannot connect to source cluster"));
            }
            if (!targetReachable) {
                return Mono.error(new MigrationException.TargetClusterUnreachableException(
                        "Cannot connect to target cluster"));
            }
            log.info("Both source and target clusters are reachable");
            return Mono.empty();
        });
    }

    private Mono<Void> discoverAndMigrateIndices(Migration migration, StartMigrationCommand command,
                                                  SourceClusterPort sourceCluster, TargetClusterPort targetCluster) {
        migration.setStatus(MigrationStatus.RUNNING);
        var options = command.getOptionsOrDefault();

        return sourceCluster.listIndices()
                .filter(indexData -> shouldMigrateIndex(indexData, command))
                .collectList()
                .flatMap(indices -> {
                    migration.setTotalIndices(indices.size());
                    log.info("Found {} indices to migrate", indices.size());

                    if (command.isDryRun()) {
                        // For dry run, just record what would be migrated
                        for (Map<String, Object> indexData : indices) {
                            String indexName = (String) indexData.get("index");
                            migration.addIndexResult(IndexMigrationResult.builder()
                                    .indexName(indexName)
                                    .success(true)
                                    .documentCount(parseLong(indexData.get("docs.count")))
                                    .build());
                        }
                        return Mono.empty();
                    }

                    return Flux.fromIterable(indices)
                            .flatMap(indexData -> migrateIndex(indexData, migration, command, sourceCluster, targetCluster),
                                    options.getMaxConcurrentIndices())
                            .then();
                });
    }

    private boolean shouldMigrateIndex(Map<String, Object> indexData, StartMigrationCommand command) {
        String indexName = (String) indexData.get("index");

        if (indexName == null) {
            return false;
        }

        // Check exclude list from command
        if (command.getExcludeIndices() != null && command.getExcludeIndices().contains(indexName)) {
            return false;
        }

        // Check include list from command (if specified, only include those)
        if (command.getIncludeIndices() != null && !command.getIncludeIndices().isEmpty()) {
            return command.getIncludeIndices().contains(indexName);
        }

        // Skip system indices (starting with .)
        if (indexName.startsWith(".")) {
            return false;
        }

        return true;
    }

    private Mono<Void> migrateIndex(Map<String, Object> indexData, Migration migration, StartMigrationCommand command,
                                    SourceClusterPort sourceCluster, TargetClusterPort targetCluster) {
        String indexName = (String) indexData.get("index");
        long startTime = System.currentTimeMillis();
        AtomicLong migratedCount = new AtomicLong(0);
        AtomicLong failedCount = new AtomicLong(0);
        var options = command.getOptionsOrDefault();

        log.info("Starting migration of index: {}", indexName);

        return targetCluster.indexExists(indexName)
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Index {} already exists in target, skipping creation", indexName);
                        return Mono.empty();
                    }
                    return createTargetIndex(indexName, sourceCluster, targetCluster);
                })
                .then(migrateDocuments(indexName, options, migratedCount, failedCount, sourceCluster, targetCluster))
                .then(targetCluster.refreshIndex(indexName))
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    long docCount = parseLong(indexData.get("docs.count"));
                    migration.addIndexResult(IndexMigrationResult.success(
                            indexName, docCount, migratedCount.get(), duration));
                    log.info("Completed migration of index {} in {}ms: {} documents",
                            indexName, duration, migratedCount.get());
                }))
                .onErrorResume(e -> {
                    log.error("Failed to migrate index {}: {}", indexName, e.getMessage());
                    migration.addIndexResult(IndexMigrationResult.failure(indexName, e.getMessage()));
                    if (options.isContinueOnFailure()) {
                        return Mono.empty();
                    }
                    return Mono.error(e);
                })
                .then();
    }

    private Mono<Void> createTargetIndex(String indexName, SourceClusterPort sourceCluster, TargetClusterPort targetCluster) {
        return Mono.zip(
                sourceCluster.getIndexSettings(indexName),
                sourceCluster.getIndexMappings(indexName)
        ).flatMap(tuple -> {
            Map<String, Object> settings = tuple.getT1();
            Map<String, Object> mappings = tuple.getT2();
            return targetCluster.createIndex(indexName, settings, mappings);
        });
    }

    private Mono<Void> migrateDocuments(String indexName, StartMigrationCommand.MigrationOptions options,
                                        AtomicLong migratedCount, AtomicLong failedCount,
                                        SourceClusterPort sourceCluster, TargetClusterPort targetCluster) {
        return sourceCluster.scrollDocuments(indexName, options.getScrollTimeout(), options.getScrollSize())
                .buffer(options.getBatchSize())
                .concatMap(batch -> targetCluster.bulkIndex(indexName, batch)
                        .doOnNext(result -> {
                            long successful = result.getItems().stream()
                                    .filter(BulkOperationResult.BulkItemResult::isSuccess)
                                    .count();
                            migratedCount.addAndGet(successful);
                            failedCount.addAndGet(batch.size() - successful);

                            if (result.isErrors()) {
                                log.warn("Bulk operation had errors for index {}: {} failed",
                                        indexName, result.getFailedDocumentIds().size());
                            }
                        }))
                .then();
    }

    private Mono<Void> migrateSavedObjects(Migration migration, StartMigrationCommand command,
                                           SourceClusterPort sourceCluster, TargetClusterPort targetCluster) {
        if (!command.isMigrateSavedObjects()) {
            log.info("Saved objects migration is disabled, skipping");
            return Mono.empty();
        }

        var options = command.getOptionsOrDefault();
        String dashboardsIndex = options.getSavedObjectsIndex();
        List<String> types = options.getSavedObjectsTypes();

        if (command.isDryRun()) {
            log.info("Dry run: would migrate saved objects from {}", dashboardsIndex);
            return Mono.empty();
        }

        long startTime = System.currentTimeMillis();
        AtomicLong migratedCount = new AtomicLong(0);

        log.info("Starting saved objects migration from {}", dashboardsIndex);

        Mono<Void> migrationFlow = targetCluster.indexExists(dashboardsIndex)
                .flatMap(exists -> {
                    if (!exists) {
                        return sourceCluster.getIndexSettings(dashboardsIndex)
                                .zipWith(sourceCluster.getIndexMappings(dashboardsIndex))
                                .flatMap(tuple -> targetCluster.createIndex(
                                        dashboardsIndex, tuple.getT1(), tuple.getT2()))
                                .onErrorResume(e -> {
                                    log.warn("Could not create dashboards index, it may not exist in source: {}",
                                            e.getMessage());
                                    return Mono.empty();
                                });
                    }
                    return Mono.empty();
                })
                .then(sourceCluster.getSavedObjects(dashboardsIndex, types)
                        .buffer(options.getBatchSize())
                        .concatMap(batch -> targetCluster.bulkIndex(dashboardsIndex, batch)
                                .doOnNext(result -> {
                                    long successful = result.getItems().stream()
                                            .filter(BulkOperationResult.BulkItemResult::isSuccess)
                                            .count();
                                    migratedCount.addAndGet(successful);
                                }))
                        .then())
                .doOnSuccess(v -> {
                    long duration = System.currentTimeMillis() - startTime;
                    migration.addIndexResult(IndexMigrationResult.builder()
                            .indexName(dashboardsIndex + " (saved objects)")
                            .success(true)
                            .migratedCount(migratedCount.get())
                            .durationMs(duration)
                            .build());
                    log.info("Completed saved objects migration: {} objects in {}ms",
                            migratedCount.get(), duration);
                })
                .doOnError(e -> {
                    log.warn("Saved objects migration failed (non-critical): {}", e.getMessage());
                    migration.addIndexResult(IndexMigrationResult.failure(
                            dashboardsIndex + " (saved objects)", e.getMessage()));
                })
                .onErrorResume(e -> Mono.empty());

        return migrationFlow;
    }

    private Mono<Migration> finalizeMigration(Migration migration) {
        migration.setCompletedAt(LocalDateTime.now());

        if (migration.getFailedIndices() > 0) {
            migration.setStatus(MigrationStatus.COMPLETED_WITH_ERRORS);
        } else {
            migration.setStatus(MigrationStatus.COMPLETED);
        }

        log.info("Migration {} completed. Status: {}, Indices: {}/{}, Documents: {}",
                migration.getId(),
                migration.getStatus(),
                migration.getCompletedIndices(),
                migration.getTotalIndices(),
                migration.getMigratedDocuments());

        return Mono.just(migration);
    }

    private Mono<Migration> handleMigrationError(Migration migration, Throwable error) {
        log.error("Migration failed: {}", error.getMessage(), error);
        migration.setStatus(MigrationStatus.FAILED);
        migration.setCompletedAt(LocalDateTime.now());
        migration.setErrorMessage(error.getMessage());
        return Mono.just(migration);
    }

    private long parseLong(Object value) {
        if (value == null) return 0;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
