package com.opensearch.utility.command.index.usecases;

import com.opensearch.utility.command.index.domain.command.DeleteIndexCommand;
import reactor.core.publisher.Mono;

public interface DeleteIndexUseCase {

    Mono<Void> execute(DeleteIndexCommand command);
}
