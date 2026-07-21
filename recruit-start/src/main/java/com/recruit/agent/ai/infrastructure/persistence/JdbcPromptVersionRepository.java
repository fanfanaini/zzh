package com.recruit.agent.ai.infrastructure.persistence;

import com.recruit.agent.ai.domain.PromptVersion;
import com.recruit.agent.ai.domain.PromptVersionRepository;
import com.recruit.agent.common.id.TimestampIdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class JdbcPromptVersionRepository implements PromptVersionRepository {
    private final JdbcTemplate jdbc;
    private final TimestampIdGenerator ids = new TimestampIdGenerator();

    public JdbcPromptVersionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public PromptVersion save(PromptVersion prompt) {
        if (prompt.enabled()) {
            jdbc.update("UPDATE prompt_version SET enabled = FALSE WHERE template_code = ?", prompt.templateCode());
        }
        jdbc.update("INSERT INTO prompt_version (id, template_code, version, system_prompt, user_prompt_template, enabled, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                prompt.id(), prompt.templateCode(), prompt.version(), prompt.systemPrompt(), prompt.userPromptTemplate(),
                prompt.enabled(), Timestamp.from(prompt.createdAt()));
        return prompt;
    }

    @Override
    public List<PromptVersion> findByTemplateCode(String code) {
        return jdbc.query("SELECT * FROM prompt_version WHERE template_code = ? ORDER BY created_at DESC", this::map, code.toUpperCase());
    }

    @Override
    public Optional<PromptVersion> findEnabled(String code) {
        return findByTemplateCode(code).stream().filter(PromptVersion::enabled).findFirst();
    }

    private PromptVersion map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new PromptVersion(rs.getLong("id"), rs.getString("template_code"), rs.getString("version"),
                rs.getString("system_prompt"), rs.getString("user_prompt_template"), rs.getBoolean("enabled"),
                rs.getTimestamp("created_at").toInstant());
    }

    @Override public long nextId() { return ids.nextId(); }
}
