package com.recruit.agent.trace;

import java.util.List;

public interface AgentTraceSink {
    void record(AgentTraceEvent event);

    List<AgentTraceEvent> findByConversationId(Long conversationId);

    List<AgentTraceEvent> findByTraceId(String traceId);

    default List<AgentTraceEvent> findRecent(int limit) {
        return List.of();
    }
}
