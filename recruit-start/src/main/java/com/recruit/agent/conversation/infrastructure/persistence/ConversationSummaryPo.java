package com.recruit.agent.conversation.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("conversation_summary")
public class ConversationSummaryPo {
    @TableId
    private Long conversationId;
    private String content;
    private Integer summarizedMessageCount;
    private Long summarizedThroughMessageId;
    private Integer tokenCount;
    private LocalDateTime updatedAt;

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getSummarizedMessageCount() { return summarizedMessageCount; }
    public void setSummarizedMessageCount(Integer summarizedMessageCount) { this.summarizedMessageCount = summarizedMessageCount; }
    public Long getSummarizedThroughMessageId() { return summarizedThroughMessageId; }
    public void setSummarizedThroughMessageId(Long summarizedThroughMessageId) { this.summarizedThroughMessageId = summarizedThroughMessageId; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
