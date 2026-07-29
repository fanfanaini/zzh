package com.recruit.agent.toolcalling;

public interface FunctionCallingToolPlanner {
    ToolCallDecision decide(ToolCallingRequest request);
}
