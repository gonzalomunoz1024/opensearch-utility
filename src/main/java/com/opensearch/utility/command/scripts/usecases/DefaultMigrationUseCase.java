package com.opensearch.utility.command.scripts.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearch.utility.command.document.domain.BulkOperationResult;
import com.opensearch.utility.command.document.domain.Document;
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
import java.util.ArrayList;
import java.util.HashMap;
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

        log.info("================================================================================");
        log.info("                     OPENSEARCH MIGRATION STARTED                              ");
        log.info("================================================================================");
        log.info("Migration ID: {}", migration.getId());
        log.info("Source: {}", command.getSource().getUrl());
        log.info("Target: {}", command.getTarget().getUrl());
        log.info("Dry Run: {}", command.isDryRun());
        log.info("Migrate Saved Objects: {}", command.isMigrateSavedObjects());
        log.info("================================================================================");

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

        log.info("================================================================================");
        log.info("                         DISCOVERING INDICES                                   ");
        log.info("================================================================================");

        return sourceCluster.listIndices()
                .filter(indexData -> shouldMigrateIndex(indexData, command))
                .collectList()
                .flatMap(indices -> {
                    migration.setTotalIndices(indices.size());

                    log.info("================================================================================");
                    log.info("                         MIGRATING USER INDICES                                ");
                    log.info("================================================================================");
                    log.info("Found {} user indices to migrate", indices.size());

                    for (Map<String, Object> indexData : indices) {
                        String indexName = (String) indexData.get("index");
                        long docCount = parseLong(indexData.get("docs.count"));
                        log.info("  - {} ({} documents)", indexName, docCount);
                    }
                    log.info("--------------------------------------------------------------------------------");

                    if (command.isDryRun()) {
                        log.info("[DRY RUN] Skipping actual migration");
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
        long docCount = parseLong(indexData.get("docs.count"));
        long startTime = System.currentTimeMillis();
        AtomicLong migratedCount = new AtomicLong(0);
        AtomicLong failedCount = new AtomicLong(0);
        var options = command.getOptionsOrDefault();

        log.info(">>> MIGRATING INDEX: {} ({} documents)", indexName, docCount);

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
                    migration.addIndexResult(IndexMigrationResult.success(
                            indexName, docCount, migratedCount.get(), duration));
                    log.info("<<< COMPLETED INDEX: {} - migrated {} documents in {}ms",
                            indexName, migratedCount.get(), duration);
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
            log.info("================================================================================");
            log.info("                    SAVED OBJECTS MIGRATION - SKIPPED                          ");
            log.info("================================================================================");
            log.info("Saved objects migration is disabled in request");
            return Mono.empty();
        }

        var options = command.getOptionsOrDefault();
        String configuredIndex = options.getSavedObjectsIndex();
        List<String> types = options.getSavedObjectsTypes();

        log.info("================================================================================");
        log.info("                    MIGRATING SAVED OBJECTS                                    ");
        log.info("================================================================================");
        log.info("Configured Index: {}", configuredIndex);
        log.info("Types to migrate:");
        for (String type : types) {
            log.info("  - {}", type);
        }
        log.info("--------------------------------------------------------------------------------");

        if (command.isDryRun()) {
            log.info("[DRY RUN] Skipping actual saved objects migration");
            return Mono.empty();
        }

        long startTime = System.currentTimeMillis();
        AtomicLong migratedCount = new AtomicLong(0);
        AtomicReference<String> actualDashboardsIndex = new AtomicReference<>(configuredIndex);

        // First, discover all dashboards indices and pick the right one
        Mono<Void> migrationFlow = sourceCluster.findDashboardsIndices()
                .collectList()
                .flatMap(foundIndices -> {
                    log.info("================================================================================");
                    log.info("                    DISCOVERING DASHBOARDS INDEX                               ");
                    log.info("================================================================================");

                    if (foundIndices.isEmpty()) {
                        log.warn("No dashboards indices found (searched for .opensearch_dashboards* and .kibana*)");
                        log.warn("Will try configured index: {}", configuredIndex);
                    } else {
                        log.info("Found {} dashboards indices:", foundIndices.size());
                        for (String idx : foundIndices) {
                            log.info("  - {}", idx);
                        }

                        // Prefer exact match, then any .opensearch_dashboards*, then .kibana*
                        String selectedIndex = configuredIndex;
                        if (foundIndices.contains(configuredIndex)) {
                            selectedIndex = configuredIndex;
                        } else if (!foundIndices.isEmpty()) {
                            // Pick the first one that matches opensearch_dashboards pattern
                            selectedIndex = foundIndices.stream()
                                    .filter(i -> i.startsWith(".opensearch_dashboards"))
                                    .findFirst()
                                    .orElse(foundIndices.get(0));
                        }
                        actualDashboardsIndex.set(selectedIndex);
                        log.info(">>> SELECTED DASHBOARDS INDEX: {}", selectedIndex);
                    }
                    log.info("--------------------------------------------------------------------------------");

                    return sourceCluster.getDocumentCount(actualDashboardsIndex.get());
                })
                .doOnNext(count -> log.info("Source dashboards index {} has {} total documents", actualDashboardsIndex.get(), count))
                .onErrorResume(e -> {
                    log.warn("Could not count documents in {}: {}", actualDashboardsIndex.get(), e.getMessage());
                    // Try to get all documents to see what's in there
                    return sourceCluster.getAllDocuments(actualDashboardsIndex.get(), 100)
                            .collectList()
                            .doOnNext(docs -> {
                                if (!docs.isEmpty()) {
                                    log.info("Sample of documents found - examining types...");
                                }
                            })
                            .thenReturn(0L);
                })
                .then(targetCluster.indexExists(actualDashboardsIndex.get()))
                .flatMap(exists -> {
                    if (!exists) {
                        log.info("Target dashboards index {} does not exist, creating it", actualDashboardsIndex.get());
                        return sourceCluster.getIndexSettings(actualDashboardsIndex.get())
                                .zipWith(sourceCluster.getIndexMappings(actualDashboardsIndex.get()))
                                .flatMap(tuple -> targetCluster.createIndex(
                                        actualDashboardsIndex.get(), tuple.getT1(), tuple.getT2()))
                                .doOnSuccess(v -> log.info("Created dashboards index {} in target", actualDashboardsIndex.get()))
                                .onErrorResume(e -> {
                                    log.warn("Could not create dashboards index, it may not exist in source: {}",
                                            e.getMessage());
                                    return Mono.empty();
                                });
                    }
                    log.info("Target dashboards index {} already exists", actualDashboardsIndex.get());
                    return Mono.empty();
                })
                .then(sourceCluster.getSavedObjects(actualDashboardsIndex.get(), types)
                        .collectList()
                        .flatMap(allDocs -> {
                            if (allDocs.isEmpty()) {
                                log.warn("================================================================================");
                                log.warn("  WARNING: No saved objects found with type filter!");
                                log.warn("  Index: {}", actualDashboardsIndex.get());
                                log.warn("  Types searched: {}", types);
                                log.warn("  Trying to fetch ALL documents to diagnose...");
                                log.warn("================================================================================");

                                // Fetch all documents to see what types exist
                                return sourceCluster.getAllDocuments(actualDashboardsIndex.get(), 1000)
                                        .collectList()
                                        .flatMap(allDocsUnfiltered -> {
                                            if (allDocsUnfiltered.isEmpty()) {
                                                log.warn("Index {} appears to be empty", actualDashboardsIndex.get());
                                                return Mono.empty();
                                            }
                                            log.info("Found {} total documents in {} (unfiltered)", allDocsUnfiltered.size(), actualDashboardsIndex.get());

                                            // These are all saved objects, migrate them all
                                            return Flux.fromIterable(allDocsUnfiltered)
                                                    .buffer(options.getBatchSize())
                                                    .concatMap(batch -> targetCluster.bulkIndex(actualDashboardsIndex.get(), batch)
                                                            .doOnNext(result -> {
                                                                long successful = result.getItems().stream()
                                                                        .filter(BulkOperationResult.BulkItemResult::isSuccess)
                                                                        .count();
                                                                migratedCount.addAndGet(successful);
                                                            }))
                                                    .then();
                                        });
                            }

                            // Separate documents by type
                            List<Document> indexPatterns = new ArrayList<>();
                            List<Document> visualizations = new ArrayList<>();
                            List<Document> dashboards = new ArrayList<>();
                            List<Document> savedSearches = new ArrayList<>();
                            List<Document> otherObjects = new ArrayList<>();

                            for (var doc : allDocs) {
                                if (doc.getSource() != null) {
                                    String type = (String) doc.getSource().get("type");
                                    if ("index-pattern".equals(type) || "index_pattern".equals(type)) {
                                        indexPatterns.add(doc);
                                    } else if ("visualization".equals(type)) {
                                        visualizations.add(doc);
                                    } else if ("dashboard".equals(type)) {
                                        dashboards.add(doc);
                                    } else if ("search".equals(type)) {
                                        savedSearches.add(doc);
                                    } else {
                                        otherObjects.add(doc);
                                    }
                                }
                            }

                            log.info("Found {} total saved objects to migrate", allDocs.size());

                            // =====================================================================
                            // INDEX PATTERNS - THE IMPORTANT STUFF
                            // =====================================================================
                            log.info("================================================================================");
                            log.info("                    MIGRATING USER INDEX PATTERNS                              ");
                            log.info("================================================================================");
                            if (indexPatterns.isEmpty()) {
                                log.warn("  NO INDEX PATTERNS FOUND!");
                            } else {
                                log.info("  Found {} index pattern(s):", indexPatterns.size());
                                for (var pattern : indexPatterns) {
                                    String title = pattern.getSource() != null ?
                                            (String) pattern.getSource().get("title") : "unknown";
                                    log.info("    - {}", title);
                                }
                            }
                            log.info("================================================================================");

                            // Other saved objects summary
                            if (!visualizations.isEmpty()) {
                                log.info("  VISUALIZATIONS: {} found", visualizations.size());
                            }
                            if (!dashboards.isEmpty()) {
                                log.info("  DASHBOARDS: {} found", dashboards.size());
                            }
                            if (!savedSearches.isEmpty()) {
                                log.info("  SAVED SEARCHES: {} found", savedSearches.size());
                            }
                            if (!otherObjects.isEmpty()) {
                                log.info("  OTHER OBJECTS: {} found", otherObjects.size());
                            }
                            log.info("--------------------------------------------------------------------------------");

                            return Flux.fromIterable(allDocs)
                                    .buffer(options.getBatchSize())
                                    .concatMap(batch -> targetCluster.bulkIndex(actualDashboardsIndex.get(), batch)
                                            .doOnNext(result -> {
                                                long successful = result.getItems().stream()
                                                        .filter(BulkOperationResult.BulkItemResult::isSuccess)
                                                        .count();
                                                migratedCount.addAndGet(successful);
                                                if (result.isErrors()) {
                                                    log.warn("Some saved objects failed to index: {}",
                                                            result.getFailedDocumentIds());
                                                }
                                            }))
                                    .then();
                        }))
                .doOnSuccess(v -> {
                    long duration = System.currentTimeMillis() - startTime;
                    migration.addIndexResult(IndexMigrationResult.builder()
                            .indexName(actualDashboardsIndex.get() + " (saved objects)")
                            .success(true)
                            .migratedCount(migratedCount.get())
                            .durationMs(duration)
                            .build());
                    log.info("<<< SAVED OBJECTS MIGRATION COMPLETED: {} objects migrated in {}ms",
                            migratedCount.get(), duration);
                })
                .doOnError(e -> {
                    log.warn("Saved objects migration failed (non-critical): {}", e.getMessage());
                    migration.addIndexResult(IndexMigrationResult.failure(
                            actualDashboardsIndex.get() + " (saved objects)", e.getMessage()));
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

        log.info("================================================================================");
        log.info("                     MIGRATION COMPLETED                                       ");
        log.info("================================================================================");
        log.info("Migration ID: {}", migration.getId());
        log.info("Status: {}", migration.getStatus());
        log.info("Total Indices: {}", migration.getTotalIndices());
        log.info("Completed Indices: {}", migration.getCompletedIndices());
        log.info("Failed Indices: {}", migration.getFailedIndices());
        log.info("Total Documents Migrated: {}", migration.getMigratedDocuments());
        if (migration.getStartedAt() != null && migration.getCompletedAt() != null) {
            log.info("Duration: {} seconds",
                    java.time.Duration.between(migration.getStartedAt(), migration.getCompletedAt()).getSeconds());
        }
        log.info("================================================================================");

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
