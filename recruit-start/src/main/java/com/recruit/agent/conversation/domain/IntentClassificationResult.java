package com.recruit.agent.conversation.domain;

import java.util.Set;

public record IntentClassificationResult(
        ReplyIntent intent,
        double confidence,
        Set<ConversationRisk> risks,
        boolean handoffRequired,
        String reason
) implements java.io.Serializable {
    public IntentClassificationResult {
        intent = intent == null ? ReplyIntent.UNKNOWN : intent;
        confidence = Math.max(0, Math.min(1, confidence));
        risks = risks == null ? Set.of() : Set.copyOf(risks);
        reason = reason == null ? "" : reason.trim();
    }
}
