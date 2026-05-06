package com.opensearch.utility.command.scripts.usecases;

import com.opensearch.utility.command.scripts.domain.Migration;
import com.opensearch.utility.command.scripts.domain.command.StartMigrationCommand;
import reactor.core.publisher.Mono;

public interface MigrationUseCase {

    Mono<Migration> execute(StartMigrationCommand command);

    Mono<Migration> getMigrationStatus(String migrationId);

    Mono<Migration> getLatestMigration();
}
