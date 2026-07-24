package com.recruit.agent.conversation.infrastructure.persistence;

import com.recruit.agent.conversation.domain.ConversationSummary;
import com.recruit.agent.conversation.domain.ConversationSummaryRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisConversationSummaryRepository implements ConversationSummaryRepository {
    private final ConversationSummaryMapper mapper;

    public MyBatisConversationSummaryRepository(ConversationSummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ConversationSummary> findByConversationId(Long conversationId) {
        return Optional.ofNullable(mapper.selectById(conversationId)).map(this::toDomain);
    }

    @Override
    public ConversationSummary save(ConversationSummary summary) {
        ConversationSummaryPo po = toPo(summary);
        if (mapper.selectById(summary.conversationId()) == null) mapper.insert(po);
        else mapper.updateById(po);
        return summary;
    }

    private ConversationSummary toDomain(ConversationSummaryPo po) {
        return new ConversationSummary(po.getConversationId(), po.getContent(), po.getSummarizedMessageCount(),
                po.getSummarizedThroughMessageId(), po.getTokenCount(),
                po.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private ConversationSummaryPo toPo(ConversationSummary summary) {
        ConversationSummaryPo po = new ConversationSummaryPo();
        po.setConversationId(summary.conversationId());
        po.setContent(summary.content());
        po.setSummarizedMessageCount(summary.summarizedMessageCount());
        po.setSummarizedThroughMessageId(summary.summarizedThroughMessageId());
        po.setTokenCount(summary.tokenCount());
        po.setUpdatedAt(summary.updatedAt().atOffset(ZoneOffset.UTC).toLocalDateTime());
        return po;
    }
}
