package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationStage;
import com.recruit.agent.conversation.domain.ConversationStatus;
import com.recruit.agent.conversation.domain.MessageSenderType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersistentConversationSummaryProviderTest {
    private final InMemoryConversationSummaryRepository repository = new InMemoryConversationSummaryRepository();
    private final HeuristicTokenEstimator estimator = new HeuristicTokenEstimator();
    private final PersistentConversationSummaryProvider provider =
            new PersistentConversationSummaryProvider(repository, estimator);

    @Test
    void incrementallyPersistsOnlyNewlyOmittedMessages() {
        List<ConversationMessage> messages = List.of(message(1L, "first"), message(2L, "second"),
                message(3L, "third"));

        provider.summarize(conversation(), messages.subList(0, 2), 100);
        String updated = provider.summarize(conversation(), messages, 100);

        assertThat(updated).contains("first", "second", "third");
        assertThat(count(updated, "first")).isEqualTo(1);
        assertThat(repository.findByConversationId(3L)).get().satisfies(summary -> {
            assertThat(summary.summarizedMessageCount()).isEqualTo(3);
            assertThat(summary.summarizedThroughMessageId()).isEqualTo(3L);
            assertThat(summary.tokenCount()).isLessThanOrEqualTo(100);
        });
    }

    private long count(String value, String fragment) {
        return value.lines().filter(line -> line.contains(fragment)).count();
    }

    private ConversationMessage message(Long id, String content) {
        return new ConversationMessage(id, 3L, MessageSenderType.CANDIDATE, content, Instant.ofEpochSecond(id));
    }

    private Conversation conversation() {
        return new Conversation(3L, 1L, 2L, ConversationStage.INITIAL_CONTACT,
                ConversationStatus.OPEN, Instant.EPOCH);
    }
}
