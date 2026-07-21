package com.recruit.agent.conversation.domain;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.job.domain.Job;

public record IntentClassificationRequest(
        Job job,
        Candidate candidate,
        Conversation conversation,
        ConversationMemory memory
) {
    public IntentClassificationRequest {
        if (job == null || candidate == null || conversation == null || memory == null) {
            throw new IllegalArgumentException("Intent classification context cannot be null");
        }
    }
}
