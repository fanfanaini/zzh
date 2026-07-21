package com.recruit.agent.conversation.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationMessageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("mysql")
public class MyBatisConversationMessageRepository implements ConversationMessageRepository {

    private final ConversationMessageMapper mapper;
    private final ConversationMessagePoConverter converter = new ConversationMessagePoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisConversationMessageRepository(ConversationMessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ConversationMessage save(ConversationMessage message) {
        if (mapper.selectById(message.id()) == null) {
            mapper.insert(converter.toPo(message));
        } else {
            mapper.updateById(converter.toPo(message));
        }
        return message;
    }

    @Override
    public List<ConversationMessage> findByConversationId(Long conversationId) {
        return mapper.selectList(Wrappers.<ConversationMessagePo>lambdaQuery()
                        .eq(ConversationMessagePo::getConversationId, conversationId)
                        .orderByAsc(ConversationMessagePo::getCreatedAt)
                        .orderByAsc(ConversationMessagePo::getId))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public long nextMessageId() {
        return idGenerator.nextId();
    }
}
