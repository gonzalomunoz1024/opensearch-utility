package com.opensearch.utility.command.index.domain.event;

import com.opensearch.utility.core.domain.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IndexDeletedEvent extends BaseEvent {

    private String indexName;
}
