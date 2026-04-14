package com.opensearch.utility.command.index.usecases;

import com.opensearch.utility.command.index.domain.command.ReindexCommand;
import com.opensearch.utility.command.index.domain.dto.outbound.ReindexResponse;
import reactor.core.publisher.Mono;

public interface ReindexUseCase {

    Mono<ReindexResponse> execute(ReindexCommand command);
}
