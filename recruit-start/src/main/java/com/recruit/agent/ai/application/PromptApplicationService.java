package com.recruit.agent.ai.application;

import com.recruit.agent.ai.domain.PromptVersion;
import com.recruit.agent.ai.domain.PromptVersionRepository;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

public class PromptApplicationService {
    private final PromptVersionRepository repository;

    public PromptApplicationService(PromptVersionRepository repository) {
        this.repository = repository;
    }

    public PromptVersion create(String code, String version, String systemPrompt, String userPromptTemplate, boolean enabled) {
        return repository.save(new PromptVersion(repository.nextId(), code, version, systemPrompt,
                userPromptTemplate, enabled, Instant.now()));
    }

    public List<PromptVersion> list(String code) {
        return repository.findByTemplateCode(code);
    }

    public PromptVersion enabled(String code) {
        return repository.findEnabled(code)
                .orElseThrow(() -> new NoSuchElementException("未配置启用的 Prompt: " + code));
    }
}
