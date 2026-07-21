package com.recruit.agent.screening.api;

import jakarta.validation.constraints.NotBlank;

public record HumanReviewRequest(
        @NotBlank(message = "人工复核结果不能为空")
        String humanResult
) {
}
