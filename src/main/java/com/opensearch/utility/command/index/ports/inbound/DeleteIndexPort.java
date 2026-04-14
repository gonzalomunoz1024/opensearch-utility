package com.opensearch.utility.command.index.ports.inbound;

import com.opensearch.utility.command.index.domain.command.DeleteIndexCommand;
import reactor.core.publisher.Mono;

public interface DeleteIndexPort {

    Mono<Void> deleteIndex(DeleteIndexCommand command);
}
