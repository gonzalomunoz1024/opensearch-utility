package com.opensearch.utility.core.cron;

import com.opensearch.utility.command.cluster.usecases.DefaultGetClusterHealthUseCase;
import com.opensearch.utility.core.config.SchedulerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduler.health-check.enabled", havingValue = "true", matchIfMissing = true)
public class ClusterHealthScheduler {

    private final DefaultGetClusterHealthUseCase clusterHealthUseCase;
    private final SchedulerConfig schedulerConfig;

    @Scheduled(cron = "${scheduler.health-check.cron:0 */5 * * * *}")
    public void scheduledHealthCheck() {
        log.debug("Running scheduled cluster health check");

        clusterHealthUseCase.checkHealthScheduled()
                .timeout(Duration.ofSeconds(schedulerConfig.getHealthCheck().getTimeoutSeconds()))
                .subscribe(
                        health -> log.debug("Scheduled health check completed: {}", health.getStatus()),
                        error -> log.error("Scheduled health check failed: {}", error.getMessage())
                );
    }
}
