package com.opensearch.utility.command.cluster.domain.event;

import com.opensearch.utility.command.cluster.domain.ClusterHealth;
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
public class ClusterHealthCheckedEvent extends BaseEvent {

    private ClusterHealth clusterHealth;
    private boolean scheduledCheck;
}
