package com.recruit.agent.workflow.execution;

import java.util.Map;

public record AgentNodeResult(Map<String, Object> updates, Map<String, Object> traceDetails) {
    public AgentNodeResult {
        updates = updates == null ? Map.of() : Map.copyOf(updates);
        traceDetails = traceDetails == null ? Map.of() : Map.copyOf(traceDetails);
    }
}
