package com.recruit.agent.vector.api;

import jakarta.validation.constraints.NotBlank;

public record IndexResumeProjectRequest(
        Long projectId,
        @NotBlank(message = "项目内容不能为空")
        String content
) {
}
