package com.recruit.agent.workflow.execution;

public interface AgentExecutionContextFactory {
    AgentExecutionContext create(Long conversationId);
}
