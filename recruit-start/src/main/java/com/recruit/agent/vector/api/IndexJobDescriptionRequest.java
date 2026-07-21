package com.recruit.agent.vector.api;

import jakarta.validation.constraints.NotBlank;

public record IndexJobDescriptionRequest(
        @NotBlank(message = "岗位描述不能为空")
        String content
) {
}
