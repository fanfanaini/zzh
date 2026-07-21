package com.recruit.agent.conversation.api;

import com.recruit.agent.conversation.domain.MessageSenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMessageRequest(
        @NotNull(message = "发送方类型不能为空")
        MessageSenderType senderType,
        @NotBlank(message = "消息内容不能为空")
        String content
) {
}
