package com.opensearch.utility.command.scripts.ports.inbound;

import com.opensearch.utility.command.scripts.domain.Migration;
import com.opensearch.utility.command.scripts.domain.command.StartMigrationCommand;
import reactor.core.publisher.Mono;

public interface MigrationPort {

    Mono<Migration> startMigration(StartMigrationCommand command);

    Mono<Migration> getMigrationStatus(String migrationId);

    Mono<Migration> getLatestMigration();
}
