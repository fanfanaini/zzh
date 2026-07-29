package com.recruit.agent.toolcalling;

public class NoopFunctionCallingToolPlanner implements FunctionCallingToolPlanner {
    @Override
    public ToolCallDecision decide(ToolCallingRequest request) {
        return ToolCallDecision.none("function calling planner is disabled");
    }
}
