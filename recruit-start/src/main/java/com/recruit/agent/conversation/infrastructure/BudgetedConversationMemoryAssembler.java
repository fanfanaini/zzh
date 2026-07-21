package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMemory;
import com.recruit.agent.conversation.domain.ConversationMemoryAssembler;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationSummaryProvider;
import com.recruit.agent.job.domain.Job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BudgetedConversationMemoryAssembler implements ConversationMemoryAssembler {

    public static final int DEFAULT_CHARACTER_BUDGET = 6000;
    public static final int DEFAULT_RECENT_MESSAGE_LIMIT = 12;

    private final int characterBudget;
    private final int recentMessageLimit;
    private final ConversationSummaryProvider summaryProvider;

    public BudgetedConversationMemoryAssembler() {
        this(DEFAULT_CHARACTER_BUDGET, DEFAULT_RECENT_MESSAGE_LIMIT, (messages, maximumCharacters) -> "");
    }

    public BudgetedConversationMemoryAssembler(int characterBudget, int recentMessageLimit,
                                               ConversationSummaryProvider summaryProvider) {
        if (characterBudget < 256) {
            throw new IllegalArgumentException("Memory character budget must be at least 256");
        }
        if (recentMessageLimit < 1) {
            throw new IllegalArgumentException("Recent message limit must be positive");
        }
        this.characterBudget = characterBudget;
        this.recentMessageLimit = recentMessageLimit;
        this.summaryProvider = summaryProvider == null ? (messages, maximumCharacters) -> "" : summaryProvider;
    }

    @Override
    public ConversationMemory assemble(Job job, Candidate candidate, Conversation conversation,
                                       List<ConversationMessage> messages) {
        List<ConversationMessage> source = messages == null ? List.of() : List.copyOf(messages);
        int metadataBudget = characterBudget / 2;
        Map<String, String> facts = boundedMap(facts(job, candidate), metadataBudget * 2 / 3);
        Map<String, String> state = boundedMap(state(conversation), metadataBudget - mapCharacters(facts));
        int reserved = mapCharacters(facts) + mapCharacters(state);
        int messageBudget = Math.max(0, characterBudget - reserved);

        List<ConversationMessage> recentReversed = new ArrayList<>();
        int usedByMessages = 0;
        for (int index = source.size() - 1; index >= 0 && recentReversed.size() < recentMessageLimit; index--) {
            ConversationMessage message = source.get(index);
            int cost = messageCost(message);
            if (cost > messageBudget - usedByMessages) {
                break;
            }
            recentReversed.add(message);
            usedByMessages += cost;
        }
        java.util.Collections.reverse(recentReversed);

        int omittedCount = source.size() - recentReversed.size();
        int summaryBudget = Math.max(0, characterBudget - reserved - usedByMessages);
        String summary = "";
        if (omittedCount > 0 && summaryBudget > 0) {
            summary = truncate(summaryProvider.summarize(source.subList(0, omittedCount), summaryBudget), summaryBudget);
        }
        int total = Math.min(characterBudget, reserved + usedByMessages + summary.length());
        return new ConversationMemory(recentReversed, summary, facts, state, total, omittedCount > 0);
    }

    private Map<String, String> facts(Job job, Candidate candidate) {
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("jobName", job.name());
        facts.put("requiredSkills", String.join(",", job.requiredSkills()));
        facts.put("candidateName", candidate.name());
        facts.put("candidateSkills", String.join(",", candidate.skills()));
        facts.put("experienceYears", String.valueOf(candidate.totalExperienceYears()));
        facts.put("expectedCity", candidate.expectedCity() == null ? "" : candidate.expectedCity());
        return java.util.Collections.unmodifiableMap(facts);
    }

    private Map<String, String> state(Conversation conversation) {
        Map<String, String> state = new LinkedHashMap<>();
        state.put("stage", conversation.stage().name());
        state.put("status", conversation.status().name());
        if (conversation.lastCandidateReplyAt() != null) {
            state.put("lastCandidateReplyAt", conversation.lastCandidateReplyAt().toString());
        }
        if (conversation.nextFollowUpAt() != null) {
            state.put("nextFollowUpAt", conversation.nextFollowUpAt().toString());
        }
        return java.util.Collections.unmodifiableMap(state);
    }

    private int mapCharacters(Map<String, String> values) {
        return values.entrySet().stream().mapToInt(entry -> entry.getKey().length() + entry.getValue().length() + 2).sum();
    }

    private Map<String, String> boundedMap(Map<String, String> source, int maximumCharacters) {
        Map<String, String> result = new LinkedHashMap<>();
        int remaining = Math.max(0, maximumCharacters);
        for (Map.Entry<String, String> entry : source.entrySet()) {
            int overhead = entry.getKey().length() + 2;
            if (remaining <= overhead) {
                break;
            }
            String value = truncate(entry.getValue(), remaining - overhead);
            result.put(entry.getKey(), value);
            remaining -= overhead + value.length();
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    private int messageCost(ConversationMessage message) {
        return message.senderType().name().length() + message.content().length() + 2;
    }

    private String truncate(String value, int maximumCharacters) {
        if (value == null || value.isBlank() || maximumCharacters == 0) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maximumCharacters ? trimmed : trimmed.substring(0, maximumCharacters);
    }
}
