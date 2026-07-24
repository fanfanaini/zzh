package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.MessageSenderType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMessageCompactorTest {

    @Test
    void preservesSystemEvidenceAndNewestConversationWithinTokenBudget() {
        HeuristicTokenEstimator estimator = new HeuristicTokenEstimator();
        List<ConversationMessage> messages = List.of(
                message(1L, MessageSenderType.SYSTEM, "old-summary-" + "x".repeat(80)
                        + " verified-tool-context"),
                message(2L, MessageSenderType.CANDIDATE, "old candidate message " + "x".repeat(50)),
                message(3L, MessageSenderType.HR, "newest reply " + "y".repeat(30))
        );

        List<ConversationMessage> compacted = new ConversationMessageCompactor(estimator).compact(messages, 32);

        assertThat(compacted).first().satisfies(message -> {
            assertThat(message.senderType()).isEqualTo(MessageSenderType.SYSTEM);
            assertThat(message.content()).contains("verified-tool-context");
        });
        assertThat(compacted).last().extracting(ConversationMessage::id).isEqualTo(3L);
        int tokens = compacted.stream().mapToInt(message ->
                estimator.estimate(message.senderType() + ": " + message.content())).sum();
        assertThat(tokens).isLessThanOrEqualTo(32);
    }

    private ConversationMessage message(Long id, MessageSenderType type, String content) {
        return new ConversationMessage(id, 3L, type, content, Instant.ofEpochSecond(id));
    }
}
