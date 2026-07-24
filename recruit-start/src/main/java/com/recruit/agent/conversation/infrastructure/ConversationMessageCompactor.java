package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.conversation.domain.TokenEstimator;

import java.util.ArrayList;
import java.util.List;

public class ConversationMessageCompactor {
    private final TokenEstimator tokenEstimator;

    public ConversationMessageCompactor(TokenEstimator tokenEstimator) {
        this.tokenEstimator = tokenEstimator;
    }

    public List<ConversationMessage> compact(List<ConversationMessage> messages, int maximumTokens) {
        if (messages == null || messages.isEmpty() || maximumTokens <= 0) return List.of();
        ConversationMessage system = messages.stream()
                .filter(message -> message.senderType() == MessageSenderType.SYSTEM)
                .reduce((first, second) -> second).orElse(null);
        List<ConversationMessage> result = new ArrayList<>();
        int used = 0;
        if (system != null) {
            int systemBudget = Math.max(1, maximumTokens / 2);
            int overhead = tokenEstimator.estimate(MessageSenderType.SYSTEM.name() + ": ");
            String content = truncateTail(system.content(), Math.max(0, systemBudget - overhead));
            if (!content.isBlank()) {
                ConversationMessage compactedSystem = new ConversationMessage(system.id(), system.conversationId(),
                        system.senderType(), content, system.createdAt());
                result.add(compactedSystem);
                used += messageTokens(compactedSystem);
            }
        }

        List<ConversationMessage> recentReversed = new ArrayList<>();
        for (int index = messages.size() - 1; index >= 0; index--) {
            ConversationMessage message = messages.get(index);
            if (message.senderType() == MessageSenderType.SYSTEM) continue;
            int remaining = maximumTokens - used;
            int overhead = tokenEstimator.estimate(message.senderType().name() + ": ");
            if (remaining <= overhead) break;
            int cost = messageTokens(message);
            if (cost <= remaining) {
                recentReversed.add(message);
                used += cost;
                continue;
            }
            String content = tokenEstimator.truncate(message.content(), remaining - overhead);
            if (!content.isBlank()) {
                recentReversed.add(new ConversationMessage(message.id(), message.conversationId(),
                        message.senderType(), content, message.createdAt()));
            }
            break;
        }
        java.util.Collections.reverse(recentReversed);
        result.addAll(recentReversed);
        return List.copyOf(result);
    }

    private int messageTokens(ConversationMessage message) {
        return tokenEstimator.estimate(message.senderType().name() + ": " + message.content());
    }

    private String truncateTail(String text, int maximumTokens) {
        if (text == null || text.isBlank() || maximumTokens <= 0) return "";
        String value = text.trim();
        if (tokenEstimator.estimate(value) <= maximumTokens) return value;
        int[] codePoints = value.codePoints().toArray();
        int low = 0;
        int high = codePoints.length;
        while (low < high) {
            int count = (low + high + 1) >>> 1;
            String candidate = new String(codePoints, codePoints.length - count, count);
            if (tokenEstimator.estimate(candidate) <= maximumTokens) low = count;
            else high = count - 1;
        }
        return new String(codePoints, codePoints.length - low, low).trim();
    }
}
