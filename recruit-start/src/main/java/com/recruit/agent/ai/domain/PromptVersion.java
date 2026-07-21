package com.recruit.agent.ai.domain;

import java.time.Instant;

public record PromptVersion(
        Long id,
        String templateCode,
        String version,
        String systemPrompt,
        String userPromptTemplate,
        boolean enabled,
        Instant createdAt
) {
    public PromptVersion {
        if (id == null || templateCode == null || templateCode.isBlank() || version == null || version.isBlank()) {
            throw new IllegalArgumentException("Prompt ID、编码和版本不能为空");
        }
        if (systemPrompt == null || systemPrompt.isBlank() || userPromptTemplate == null || userPromptTemplate.isBlank()) {
            throw new IllegalArgumentException("Prompt 内容不能为空");
        }
        templateCode = templateCode.trim().toUpperCase();
        version = version.trim();
        if (createdAt == null) createdAt = Instant.now();
    }
}
