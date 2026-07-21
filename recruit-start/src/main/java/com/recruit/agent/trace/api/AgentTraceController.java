package com.recruit.agent.trace.api;

import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceSink;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent-traces")
public class AgentTraceController {
    private final AgentTraceSink traceSink;

    public AgentTraceController(AgentTraceSink traceSink) {
        this.traceSink = traceSink;
    }

    @GetMapping
    public List<AgentTraceResponse> find(
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) String traceId
    ) {
        if (conversationId == null && (traceId == null || traceId.isBlank())) {
            throw new IllegalArgumentException("conversationId or traceId is required");
        }
        List<AgentTraceEvent> events = traceId == null || traceId.isBlank()
                ? traceSink.findByConversationId(conversationId)
                : traceSink.findByTraceId(traceId.trim());
        return events.stream().map(AgentTraceResponse::from).toList();
    }
}
