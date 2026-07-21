package com.recruit.agent.ai.api;

import jakarta.validation.constraints.NotBlank;

public record CreatePromptVersionRequest(
        @NotBlank String version,
        @NotBlank String systemPrompt,
        @NotBlank String userPromptTemplate,
        boolean enabled
) {}
