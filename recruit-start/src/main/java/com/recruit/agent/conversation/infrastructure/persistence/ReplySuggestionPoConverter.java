package com.recruit.agent.conversation.infrastructure.persistence;

import com.recruit.agent.conversation.domain.ReplyIntent;
import com.recruit.agent.conversation.domain.ReplySuggestion;
import com.recruit.agent.conversation.domain.ReplySuggestionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ReplySuggestionPoConverter {

    public ReplySuggestionPo toPo(ReplySuggestion suggestion) {
        ReplySuggestionPo po = new ReplySuggestionPo();
        po.setId(suggestion.id());
        po.setConversationId(suggestion.conversationId());
        po.setIntent(suggestion.intent().name());
        po.setConfidence(BigDecimal.valueOf(suggestion.confidence()));
        po.setNeedHuman(suggestion.needHuman());
        po.setSuggestedReply(suggestion.suggestedReply());
        po.setStatus(suggestion.status().name());
        po.setCreatedAt(LocalDateTime.ofInstant(suggestion.createdAt(), ZoneOffset.UTC));
        return po;
    }

    public ReplySuggestion toDomain(ReplySuggestionPo po) {
        return new ReplySuggestion(
                po.getId(),
                po.getConversationId(),
                ReplyIntent.valueOf(po.getIntent()),
                po.getConfidence().doubleValue(),
                Boolean.TRUE.equals(po.getNeedHuman()),
                po.getSuggestedReply(),
                ReplySuggestionStatus.valueOf(po.getStatus()),
                po.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
