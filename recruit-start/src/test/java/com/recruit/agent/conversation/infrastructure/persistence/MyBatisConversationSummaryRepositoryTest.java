package com.recruit.agent.conversation.infrastructure.persistence;

import com.recruit.agent.conversation.domain.ConversationSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MyBatisConversationSummaryRepositoryTest {
    private final ConversationSummaryMapper mapper = mock(ConversationSummaryMapper.class);
    private final MyBatisConversationSummaryRepository repository = new MyBatisConversationSummaryRepository(mapper);

    @Test
    void insertsAndRestoresPersistentSummary() {
        ConversationSummary summary = new ConversationSummary(3L, "CANDIDATE: hello", 2, 8L, 5,
                Instant.parse("2026-07-21T08:00:00Z"));

        repository.save(summary);

        ArgumentCaptor<ConversationSummaryPo> captor = ArgumentCaptor.forClass(ConversationSummaryPo.class);
        verify(mapper).insert(captor.capture());
        ConversationSummaryPo po = captor.getValue();
        assertThat(po.getConversationId()).isEqualTo(3L);
        assertThat(po.getSummarizedMessageCount()).isEqualTo(2);
        assertThat(po.getTokenCount()).isEqualTo(5);

        when(mapper.selectById(3L)).thenReturn(po);
        assertThat(repository.findByConversationId(3L)).contains(summary);
    }
}
