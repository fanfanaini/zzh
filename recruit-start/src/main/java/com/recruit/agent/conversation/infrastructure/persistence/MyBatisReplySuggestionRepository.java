package com.recruit.agent.conversation.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.conversation.domain.ReplySuggestion;
import com.recruit.agent.conversation.domain.ReplySuggestionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisReplySuggestionRepository implements ReplySuggestionRepository {

    private final ReplySuggestionMapper mapper;
    private final ReplySuggestionPoConverter converter = new ReplySuggestionPoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisReplySuggestionRepository(ReplySuggestionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ReplySuggestion save(ReplySuggestion suggestion) {
        if (mapper.selectById(suggestion.id()) == null) {
            mapper.insert(converter.toPo(suggestion));
        } else {
            mapper.updateById(converter.toPo(suggestion));
        }
        return suggestion;
    }

    @Override
    public Optional<ReplySuggestion> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(converter::toDomain);
    }

    @Override
    public List<ReplySuggestion> findByConversationId(Long conversationId) {
        return mapper.selectList(Wrappers.<ReplySuggestionPo>lambdaQuery()
                        .eq(ReplySuggestionPo::getConversationId, conversationId)
                        .orderByDesc(ReplySuggestionPo::getCreatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public long nextSuggestionId() {
        return idGenerator.nextId();
    }
}
