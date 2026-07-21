package com.recruit.agent.conversation.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisConversationRepository implements ConversationRepository {

    private final ConversationMapper mapper;
    private final ConversationPoConverter converter = new ConversationPoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisConversationRepository(ConversationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Conversation save(Conversation conversation) {
        if (mapper.selectById(conversation.id()) == null) {
            mapper.insert(converter.toPo(conversation));
        } else {
            mapper.updateById(converter.toPo(conversation));
        }
        return conversation;
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(converter::toDomain);
    }

    @Override
    public List<Conversation> findAll() {
        return mapper.selectList(Wrappers.<ConversationPo>lambdaQuery()
                        .orderByDesc(ConversationPo::getCreatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public List<Conversation> findByJobId(Long jobId) {
        return mapper.selectList(Wrappers.<ConversationPo>lambdaQuery()
                        .eq(ConversationPo::getJobId, jobId)
                        .orderByDesc(ConversationPo::getCreatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public List<Conversation> findByCandidateId(Long candidateId) {
        return mapper.selectList(Wrappers.<ConversationPo>lambdaQuery()
                        .eq(ConversationPo::getCandidateId, candidateId)
                        .orderByDesc(ConversationPo::getCreatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public long nextConversationId() {
        return idGenerator.nextId();
    }
}
