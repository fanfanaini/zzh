package com.recruit.agent.ai.infrastructure;

import com.recruit.agent.ai.domain.PromptVersion;
import com.recruit.agent.ai.domain.PromptVersionRepository;
import com.recruit.agent.common.id.TimestampIdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!mysql")
public class InMemoryPromptVersionRepository implements PromptVersionRepository {
    private final Map<Long, PromptVersion> storage = new ConcurrentHashMap<>();
    private final TimestampIdGenerator ids = new TimestampIdGenerator();

    @Override
    public synchronized PromptVersion save(PromptVersion prompt) {
        if (prompt.enabled()) {
            storage.replaceAll((id, current) -> current.templateCode().equals(prompt.templateCode()) && current.enabled()
                    ? new PromptVersion(current.id(), current.templateCode(), current.version(), current.systemPrompt(),
                    current.userPromptTemplate(), false, current.createdAt()) : current);
        }
        storage.put(prompt.id(), prompt);
        return prompt;
    }

    @Override
    public List<PromptVersion> findByTemplateCode(String code) {
        return storage.values().stream().filter(p -> p.templateCode().equalsIgnoreCase(code))
                .sorted(Comparator.comparing(PromptVersion::createdAt).reversed()).toList();
    }

    @Override
    public Optional<PromptVersion> findEnabled(String code) {
        return findByTemplateCode(code).stream().filter(PromptVersion::enabled).findFirst();
    }

    @Override public long nextId() { return ids.nextId(); }
}
