package com.opensearch.utility.command.document.ports.inbound;

import com.opensearch.utility.command.document.domain.command.PopulateIndexCommand;
import com.opensearch.utility.command.document.domain.dto.outbound.PopulateStatusResponse;
import reactor.core.publisher.Mono;

public interface PopulateIndexPort {

    Mono<PopulateStatusResponse> populateIndex(PopulateIndexCommand command);
}
