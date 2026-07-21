package com.recruit.agent.knowledge.api;

import jakarta.validation.constraints.NotBlank;

public record IndexKnowledgeRequest(@NotBlank String sourceName, @NotBlank String content) {}
