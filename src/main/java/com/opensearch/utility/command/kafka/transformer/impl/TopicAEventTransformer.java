package com.opensearch.utility.command.kafka.transformer.impl;

import com.opensearch.utility.command.kafka.transformer.AbstractEventTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Transformer for topic-a: ResourceGuardrailsRequestedEvent
 *
 * Schema structure:
 * - apiVersion: root level
 * - kind: root level
 * - metadata.source.service: source service name
 * - metadata.correlationId: correlation ID
 * - status.state: current state
 * - spec.target.environment: target environment
 * - spec.target.cluster: target cluster
 * - spec.metadata.organization: line of business
 * - spec.metadata.appId: application ID
 * - spec.metadata.workflow.name: workflow name
 */
@Component
public class TopicAEventTransformer extends AbstractEventTransformer {

    public TopicAEventTransformer() {
        super("topic-a");
    }

    @Override
    protected String extractApiVersion(Map<String, Object> event) {
        // "policy.lightspeed..net/v1"
        return extractStringOrDefault(event, "v1", "apiVersion");
    }

    @Override
    protected String extractKind(Map<String, Object> event) {
        // "ResourceGuardrailsRequestedEvent"
        return extractStringOrDefault(event, "unknown", "kind");
    }

    @Override
    protected String extractSource(Map<String, Object> event) {
        // "github-actions"
        return extractStringOrDefault(event, "unknown", "metadata", "source", "service");
    }

    @Override
    protected String extractCorrelationId(Map<String, Object> event) {
        // "abc1"
        return extractStringOrDefault(event, "", "metadata", "correlationId");
    }

    @Override
    protected String extractStatus(Map<String, Object> event) {
        // "POLICY_REQUESTED"
        return extractStringOrDefault(event, "unknown", "status", "state");
    }

    @Override
    protected String extractEnvironment(Map<String, Object> event) {
        // "dev"
        return extractStringOrDefault(event, "unknown", "spec", "target", "environment");
    }

    @Override
    protected String extractCluster(Map<String, Object> event) {
        // "CC"
        return extractStringOrDefault(event, "unknown", "spec", "target", "cluster");
    }

    @Override
    protected String extractLob(Map<String, Object> event) {
        // "CTO" (organization)
        return extractStringOrDefault(event, "unknown", "spec", "metadata", "organization");
    }

    @Override
    protected String extractApplicationId(Map<String, Object> event) {
        // "CLAUT"
        return extractStringOrDefault(event, "unknown", "spec", "metadata", "appId");
    }

    @Override
    protected String extractWorkflowName(Map<String, Object> event) {
        // "lightspeed-pr-workflow"
        return extractStringOrDefault(event, "unknown", "spec", "metadata", "workflow", "name");
    }
}
