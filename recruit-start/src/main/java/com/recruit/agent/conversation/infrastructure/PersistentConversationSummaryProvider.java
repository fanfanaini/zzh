package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationSummary;
import com.recruit.agent.conversation.domain.ConversationSummaryProvider;
import com.recruit.agent.conversation.domain.ConversationSummaryRepository;
import com.recruit.agent.conversation.domain.TokenEstimator;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class PersistentConversationSummaryProvider implements ConversationSummaryProvider {
    private final ConversationSummaryRepository repository;
    private final TokenEstimator tokenEstimator;

    public PersistentConversationSummaryProvider(ConversationSummaryRepository repository,
                                                 TokenEstimator tokenEstimator) {
        this.repository = repository;
        this.tokenEstimator = tokenEstimator;
    }

    @Override
    public String summarize(Conversation conversation, List<ConversationMessage> messages, int maximumTokens) {
        if (messages == null || messages.isEmpty() || maximumTokens <= 0) return "";
        ConversationSummary existing = repository.findByConversationId(conversation.id()).orElse(null);
        boolean canAppend = hasMatchingPrefix(existing, messages);
        int newMessageStart = canAppend ? existing.summarizedMessageCount() : 0;
        String previous = canAppend ? existing.content() : "";
        String additions = messages.subList(newMessageStart, messages.size()).stream()
                .map(message -> message.senderType().name() + ": " + normalize(message.content()))
                .collect(Collectors.joining("\n"));
        String combined = previous.isBlank() ? additions
                : additions.isBlank() ? previous : previous + "\n" + additions;
        String compacted = tokenEstimator.truncate(combined, maximumTokens);
        Long throughMessageId = messages.get(messages.size() - 1).id();
        ConversationSummary saved = new ConversationSummary(conversation.id(), compacted, messages.size(),
                throughMessageId, tokenEstimator.estimate(compacted), Instant.now());
        repository.save(saved);
        return saved.content();
    }

    private boolean hasMatchingPrefix(ConversationSummary existing, List<ConversationMessage> messages) {
        if (existing == null || existing.summarizedMessageCount() <= 0
                || existing.summarizedMessageCount() > messages.size()) return false;
        return java.util.Objects.equals(existing.summarizedThroughMessageId(),
                messages.get(existing.summarizedMessageCount() - 1).id());
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}
