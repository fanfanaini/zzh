package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMemory;
import com.recruit.agent.conversation.domain.ConversationMemoryAssembler;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationSummaryProvider;
import com.recruit.agent.conversation.domain.TokenEstimator;
import com.recruit.agent.job.domain.Job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BudgetedConversationMemoryAssembler implements ConversationMemoryAssembler {
    public static final int DEFAULT_TOKEN_BUDGET = 2000;
    public static final int DEFAULT_RECENT_MESSAGE_LIMIT = 12;

    private final int tokenBudget;
    private final int recentMessageLimit;
    private final ConversationSummaryProvider summaryProvider;
    private final TokenEstimator tokenEstimator;

    public BudgetedConversationMemoryAssembler() {
        this(DEFAULT_TOKEN_BUDGET, DEFAULT_RECENT_MESSAGE_LIMIT,
                (conversation, messages, maximumTokens) -> "", new HeuristicTokenEstimator());
    }

    public BudgetedConversationMemoryAssembler(int tokenBudget, int recentMessageLimit,
                                               ConversationSummaryProvider summaryProvider) {
        this(tokenBudget, recentMessageLimit, summaryProvider, new HeuristicTokenEstimator());
    }

    public BudgetedConversationMemoryAssembler(int tokenBudget, int recentMessageLimit,
                                               ConversationSummaryProvider summaryProvider,
                                               TokenEstimator tokenEstimator) {
        if (tokenBudget < 64) throw new IllegalArgumentException("Memory token budget must be at least 64");
        if (recentMessageLimit < 1) throw new IllegalArgumentException("Recent message limit must be positive");
        this.tokenBudget = tokenBudget;
        this.recentMessageLimit = recentMessageLimit;
        this.summaryProvider = summaryProvider == null
                ? (conversation, messages, maximumTokens) -> "" : summaryProvider;
        this.tokenEstimator = tokenEstimator == null ? new HeuristicTokenEstimator() : tokenEstimator;
    }

    @Override
    public ConversationMemory assemble(Job job, Candidate candidate, Conversation conversation,
                                       List<ConversationMessage> messages) {
        List<ConversationMessage> source = messages == null ? List.of() : List.copyOf(messages);
        int metadataBudget = Math.max(16, tokenBudget / 3);
        Map<String, String> facts = boundedMap(facts(job, candidate), metadataBudget * 2 / 3);
        Map<String, String> state = boundedMap(state(conversation),
                Math.max(0, metadataBudget - mapTokens(facts)));
        int metadataTokens = mapTokens(facts) + mapTokens(state);
        int available = Math.max(0, tokenBudget - metadataTokens);
        int recentBudget = available * 3 / 4;

        List<ConversationMessage> recentReversed = new ArrayList<>();
        int usedByMessages = 0;
        for (int index = source.size() - 1; index >= 0 && recentReversed.size() < recentMessageLimit; index--) {
            ConversationMessage message = source.get(index);
            int remaining = recentBudget - usedByMessages;
            int cost = messageTokens(message);
            if (cost <= remaining) {
                recentReversed.add(message);
                usedByMessages += cost;
                continue;
            }
            if (recentReversed.isEmpty() && remaining > 1) {
                int overhead = tokenEstimator.estimate(message.senderType().name() + ": ");
                String content = tokenEstimator.truncate(message.content(), Math.max(0, remaining - overhead));
                if (!content.isBlank()) {
                    ConversationMessage compacted = new ConversationMessage(message.id(), message.conversationId(),
                            message.senderType(), content, message.createdAt());
                    recentReversed.add(compacted);
                    usedByMessages += messageTokens(compacted);
                }
            }
            break;
        }
        java.util.Collections.reverse(recentReversed);

        int omittedCount = source.size() - recentReversed.size();
        int summaryBudget = Math.max(0, available - usedByMessages);
        String summary = omittedCount == 0 || summaryBudget == 0 ? ""
                : tokenEstimator.truncate(summaryProvider.summarize(conversation,
                source.subList(0, omittedCount), summaryBudget), summaryBudget);
        int totalTokens = Math.min(tokenBudget, metadataTokens + usedByMessages + tokenEstimator.estimate(summary));
        int totalCharacters = mapCharacters(facts) + mapCharacters(state) + summary.length()
                + recentReversed.stream().mapToInt(message -> message.content().length()).sum();
        return new ConversationMemory(recentReversed, summary, facts, state,
                totalTokens, totalCharacters, omittedCount > 0);
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

    private int mapTokens(Map<String, String> values) {
        return values.entrySet().stream()
                .mapToInt(entry -> tokenEstimator.estimate(entry.getKey() + ": " + entry.getValue())).sum();
    }

    private int mapCharacters(Map<String, String> values) {
        return values.entrySet().stream()
                .mapToInt(entry -> entry.getKey().length() + entry.getValue().length() + 2).sum();
    }

    private Map<String, String> boundedMap(Map<String, String> source, int maximumTokens) {
        Map<String, String> result = new LinkedHashMap<>();
        int remaining = Math.max(0, maximumTokens);
        for (Map.Entry<String, String> entry : source.entrySet()) {
            int overhead = tokenEstimator.estimate(entry.getKey() + ": ");
            if (remaining <= overhead) break;
            String value = tokenEstimator.truncate(entry.getValue(), remaining - overhead);
            result.put(entry.getKey(), value);
            remaining -= overhead + tokenEstimator.estimate(value);
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    private int messageTokens(ConversationMessage message) {
        return tokenEstimator.estimate(message.senderType().name() + ": " + message.content());
    }
}
