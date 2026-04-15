package com.opensearch.utility.command.kafka.transformer.impl;

import com.opensearch.utility.command.kafka.transformer.AbstractEventTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Transformer for topic-b: ResourceGuardrailsResponseEvent
 *
 * Schema structure:
 * - apiVersion: root level
 * - kind: root level
 * - metadata.source.service: source service name
 * - metadata.correlationId: correlation ID
 * - metadata.environment: environment (fallback to spec.target.environment)
 * - status.state: current state
 * - spec.target.cluster: target cluster
 * - spec.organization: line of business
 * - spec.appId: application ID
 * - workflowName: not present in this schema
 */
@Component
public class TopicBEventTransformer extends AbstractEventTransformer {

    public TopicBEventTransformer() {
        super("topic-b");
    }

    @Override
    protected String extractApiVersion(Map<String, Object> event) {
        // "policy.lightspeed..net/v1"
        return extractStringOrDefault(event, "v1", "apiVersion");
    }

    @Override
    protected String extractKind(Map<String, Object> event) {
        // "ResourceGuardrailsResponseEvent"
        return extractStringOrDefault(event, "unknown", "kind");
    }

    @Override
    protected String extractSource(Map<String, Object> event) {
        // "lightspeed-guardrails-service"
        return extractStringOrDefault(event, "unknown", "metadata", "source", "service");
    }

    @Override
    protected String extractCorrelationId(Map<String, Object> event) {
        // "abc1"
        return extractStringOrDefault(event, "", "metadata", "correlationId");
    }

    @Override
    protected String extractStatus(Map<String, Object> event) {
        // "GUARDRAILS_RESPONSE"
        return extractStringOrDefault(event, "unknown", "status", "state");
    }

    @Override
    protected String extractEnvironment(Map<String, Object> event) {
        // Try metadata.environment first, fallback to spec.target.environment
        String env = extractString(event, "metadata", "environment");
        if (env != null) {
            return env;
        }
        return extractStringOrDefault(event, "unknown", "spec", "target", "environment");
    }

    @Override
    protected String extractCluster(Map<String, Object> event) {
        // "CC"
        return extractStringOrDefault(event, "unknown", "spec", "target", "cluster");
    }

    @Override
    protected String extractLob(Map<String, Object> event) {
        // "CTO"
        return extractStringOrDefault(event, "unknown", "spec", "organization");
    }

    @Override
    protected String extractApplicationId(Map<String, Object> event) {
        // "CANARY"
        return extractStringOrDefault(event, "unknown", "spec", "appId");
    }

    @Override
    protected String extractWorkflowName(Map<String, Object> event) {
        // Not present in this schema - return "N/A"
        return "N/A";
    }
}
