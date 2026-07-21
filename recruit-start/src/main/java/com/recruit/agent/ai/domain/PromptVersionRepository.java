package com.recruit.agent.ai.domain;

import java.util.List;
import java.util.Optional;

public interface PromptVersionRepository {
    PromptVersion save(PromptVersion prompt);
    List<PromptVersion> findByTemplateCode(String templateCode);
    Optional<PromptVersion> findEnabled(String templateCode);
    long nextId();
}
