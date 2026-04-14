package com.opensearch.utility.command.index.ports.inbound;

import com.opensearch.utility.command.index.domain.Index;
import com.opensearch.utility.command.index.domain.command.CreateIndexCommand;
import reactor.core.publisher.Mono;

public interface CreateIndexPort {

    Mono<Index> createIndex(CreateIndexCommand command);
}
