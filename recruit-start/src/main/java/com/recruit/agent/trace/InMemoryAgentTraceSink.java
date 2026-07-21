package com.recruit.agent.trace;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!mysql")
public final class InMemoryAgentTraceSink implements AgentTraceSink {
    private final CopyOnWriteArrayList<AgentTraceEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void record(AgentTraceEvent event) {
        events.add(event);
    }

    @Override
    public List<AgentTraceEvent> findByConversationId(Long conversationId) {
        return events.stream().filter(event -> java.util.Objects.equals(event.conversationId(), conversationId)).toList();
    }

    @Override
    public List<AgentTraceEvent> findByTraceId(String traceId) {
        return events.stream().filter(event -> event.traceId().equals(traceId)).toList();
    }
}
