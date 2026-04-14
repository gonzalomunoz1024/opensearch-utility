package com.opensearch.utility.command.document.usecases;

import com.opensearch.utility.command.document.domain.command.PopulateIndexCommand;
import com.opensearch.utility.command.document.domain.dto.outbound.PopulateStatusResponse;
import reactor.core.publisher.Mono;

public interface PopulateIndexUseCase {

    Mono<PopulateStatusResponse> execute(PopulateIndexCommand command);
}
