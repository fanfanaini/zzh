package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.MessageSenderType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryConversationMessageRepositoryTest {

    @Test
    void usesIdAsStableOrderWhenTimestampsAreEqual() {
        InMemoryConversationMessageRepository repository = new InMemoryConversationMessageRepository();
        Instant occurredAt = Instant.parse("2026-07-21T06:00:00Z");
        repository.save(new ConversationMessage(2L, 1L, MessageSenderType.HR, "second", occurredAt));
        repository.save(new ConversationMessage(1L, 1L, MessageSenderType.CANDIDATE, "first", occurredAt));

        assertThat(repository.findByConversationId(1L)).extracting(ConversationMessage::content)
                .containsExactly("first", "second");
    }
}
