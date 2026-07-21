package com.recruit.agent.conversation.domain;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.job.domain.Job;

import java.util.List;

public interface ConversationMemoryAssembler {

    ConversationMemory assemble(Job job, Candidate candidate, Conversation conversation,
                                List<ConversationMessage> messages);
}
