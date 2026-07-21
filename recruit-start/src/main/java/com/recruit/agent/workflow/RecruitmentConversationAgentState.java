package com.recruit.agent.workflow;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMemory;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationRoute;
import com.recruit.agent.conversation.domain.IntentClassificationResult;
import com.recruit.agent.conversation.domain.ReplySuggestionDraft;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.workflow.execution.AgentExecutionContext;
import org.bsc.langgraph4j.state.AgentState;

import java.util.List;
import java.util.Map;

public final class RecruitmentConversationAgentState extends AgentState {
    static final String EXECUTION_CONTEXT = "executionContext";
    static final String JOB = "job";
    static final String CANDIDATE = "candidate";
    static final String CONVERSATION = "conversation";
    static final String MESSAGES = "messages";
    static final String MEMORY = "memory";
    static final String CLASSIFICATION = "classification";
    static final String ROUTE = "route";
    static final String TOOL_CONTEXT = "toolContext";
    static final String TOOL_SUCCEEDED = "toolSucceeded";
    static final String SUGGESTION = "suggestion";

    public RecruitmentConversationAgentState(Map<String, Object> data) {
        super(data);
    }

    AgentExecutionContext executionContext() {
        return value(EXECUTION_CONTEXT).map(AgentExecutionContext.class::cast).orElseThrow();
    }
    String traceId() { return executionContext().traceId(); }
    Job job() { return value(JOB).map(Job.class::cast).orElseThrow(); }
    Candidate candidate() { return value(CANDIDATE).map(Candidate.class::cast).orElseThrow(); }
    Conversation conversation() { return value(CONVERSATION).map(Conversation.class::cast).orElseThrow(); }
    @SuppressWarnings("unchecked")
    List<ConversationMessage> messages() { return value(MESSAGES).map(value -> (List<ConversationMessage>) value).orElse(List.of()); }
    ConversationMemory memory() { return value(MEMORY).map(ConversationMemory.class::cast).orElseThrow(); }
    IntentClassificationResult classification() { return value(CLASSIFICATION).map(IntentClassificationResult.class::cast).orElseThrow(); }
    ConversationRoute route() { return value(ROUTE).map(ConversationRoute.class::cast).orElseThrow(); }
    String toolContext() { return value(TOOL_CONTEXT).map(Object::toString).orElse(""); }
    boolean toolSucceeded() { return value(TOOL_SUCCEEDED).map(Boolean.class::cast).orElse(true); }
    ReplySuggestionDraft suggestion() { return value(SUGGESTION).map(ReplySuggestionDraft.class::cast).orElseThrow(); }
}
