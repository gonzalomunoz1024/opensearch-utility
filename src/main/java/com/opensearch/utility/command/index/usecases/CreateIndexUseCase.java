package com.opensearch.utility.command.index.usecases;

import com.opensearch.utility.command.index.domain.Index;
import com.opensearch.utility.command.index.domain.command.CreateIndexCommand;
import reactor.core.publisher.Mono;

public interface CreateIndexUseCase {

    Mono<Index> execute(CreateIndexCommand command);
}
