package com.recruit.agent.toolcalling;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMemory;
import com.recruit.agent.conversation.domain.ConversationRoute;
import com.recruit.agent.job.domain.Job;

import java.util.List;

public record ToolCallingRequest(
        Job job,
        Candidate candidate,
        Conversation conversation,
        ConversationMemory memory,
        ConversationRoute route,
        List<String> allowedTools
) {
    public ToolCallingRequest {
        allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
    }
}
