package com.recruit.agent.skill.infrastructure;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.skill.domain.RecruitmentSkill;
import com.recruit.agent.skill.domain.RecruitmentSkillCard;
import com.recruit.agent.skill.domain.RecruitmentSkillRepository;
import com.recruit.agent.skill.domain.RecruitmentSkillStatus;
import com.recruit.agent.skill.domain.RecruitmentSkillVersion;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Profile("mysql")
public class JdbcRecruitmentSkillRepository implements RecruitmentSkillRepository {
    private final JdbcTemplate jdbc;
    private final TimestampIdGenerator ids = new TimestampIdGenerator();

    public JdbcRecruitmentSkillRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public RecruitmentSkill saveSkill(RecruitmentSkill skill) {
        int updated = jdbc.update("""
                        UPDATE recruitment_skill
                        SET name = ?, category = ?, description = ?, scenario_text = ?, trigger_keywords = ?,
                            priority = ?, status = ?, current_version_id = ?, updated_at = ?
                        WHERE id = ?
                        """,
                skill.name(), skill.category(), skill.description(), skill.scenario(),
                join(skill.triggerKeywords()), skill.priority(), skill.status().name(), skill.currentVersionId(),
                Timestamp.from(skill.updatedAt()), skill.id());
        if (updated == 0) {
            jdbc.update("""
                            INSERT INTO recruitment_skill
                            (id, name, category, description, scenario_text, trigger_keywords, priority,
                             status, current_version_id, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    skill.id(), skill.name(), skill.category(), skill.description(), skill.scenario(),
                    join(skill.triggerKeywords()), skill.priority(), skill.status().name(), skill.currentVersionId(),
                    Timestamp.from(skill.createdAt()), Timestamp.from(skill.updatedAt()));
        }
        return skill;
    }

    @Override
    public RecruitmentSkillVersion saveVersion(RecruitmentSkillVersion version) {
        jdbc.update("""
                        INSERT INTO recruitment_skill_version
                        (id, skill_id, version, instruction, prompt_fragment, few_shots, guardrails,
                         allowed_tools, risk_tags, handoff_policy, published, created_at, published_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                version.id(), version.skillId(), version.version(), version.instruction(), version.promptFragment(),
                version.fewShots(), version.guardrails(), join(version.allowedTools()), join(version.riskTags()),
                version.handoffPolicy(), version.published(), Timestamp.from(version.createdAt()),
                version.publishedAt() == null ? null : Timestamp.from(version.publishedAt()));
        return version;
    }

    @Override
    @Transactional
    public void publish(Long skillId, RecruitmentSkillVersion version) {
        Instant now = Instant.now();
        jdbc.update("UPDATE recruitment_skill_version SET published = FALSE WHERE skill_id = ?", skillId);
        jdbc.update("UPDATE recruitment_skill_version SET published = TRUE, published_at = ? WHERE id = ?",
                Timestamp.from(now), version.id());
        jdbc.update("""
                        UPDATE recruitment_skill
                        SET status = ?, current_version_id = ?, updated_at = ?
                        WHERE id = ?
                        """,
                RecruitmentSkillStatus.ENABLED.name(), version.id(), Timestamp.from(now), skillId);
    }

    @Override
    public Optional<RecruitmentSkill> findSkill(Long id) {
        List<RecruitmentSkill> result = jdbc.query("SELECT * FROM recruitment_skill WHERE id = ?", this::mapSkill, id);
        return result.stream().findFirst();
    }

    @Override
    public Optional<RecruitmentSkillVersion> findVersion(Long versionId) {
        List<RecruitmentSkillVersion> result = jdbc.query(
                "SELECT * FROM recruitment_skill_version WHERE id = ?", this::mapVersion, versionId);
        return result.stream().findFirst();
    }

    @Override
    public Optional<RecruitmentSkillVersion> findCurrentVersion(Long skillId) {
        List<RecruitmentSkillVersion> result = jdbc.query("""
                        SELECT v.* FROM recruitment_skill s
                        JOIN recruitment_skill_version v ON v.id = s.current_version_id
                        WHERE s.id = ?
                        """, this::mapVersion, skillId);
        return result.stream().findFirst();
    }

    @Override
    public List<RecruitmentSkillCard> enabledCards() {
        return jdbc.query("""
                        SELECT s.*, v.version AS current_version
                        FROM recruitment_skill s
                        JOIN recruitment_skill_version v ON v.id = s.current_version_id
                        WHERE s.status = 'ENABLED'
                        ORDER BY s.priority DESC, s.updated_at DESC
                        """, this::mapCard);
    }

    @Override
    public List<RecruitmentSkill> listSkills() {
        return jdbc.query("SELECT * FROM recruitment_skill ORDER BY updated_at DESC", this::mapSkill);
    }

    @Override
    public List<RecruitmentSkillVersion> listVersions(Long skillId) {
        return jdbc.query("""
                        SELECT * FROM recruitment_skill_version
                        WHERE skill_id = ?
                        ORDER BY created_at DESC
                        """, this::mapVersion, skillId);
    }

    @Override
    public long nextId() {
        return ids.nextId();
    }

    private RecruitmentSkill mapSkill(ResultSet rs, int row) throws SQLException {
        return new RecruitmentSkill(rs.getLong("id"), rs.getString("name"), rs.getString("category"),
                rs.getString("description"), rs.getString("scenario_text"), split(rs.getString("trigger_keywords")),
                rs.getInt("priority"), RecruitmentSkillStatus.valueOf(rs.getString("status")),
                nullableLong(rs, "current_version_id"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private RecruitmentSkillVersion mapVersion(ResultSet rs, int row) throws SQLException {
        Timestamp publishedAt = rs.getTimestamp("published_at");
        return new RecruitmentSkillVersion(rs.getLong("id"), rs.getLong("skill_id"), rs.getString("version"),
                rs.getString("instruction"), rs.getString("prompt_fragment"), rs.getString("few_shots"),
                rs.getString("guardrails"), split(rs.getString("allowed_tools")), split(rs.getString("risk_tags")),
                rs.getString("handoff_policy"), rs.getBoolean("published"),
                rs.getTimestamp("created_at").toInstant(), publishedAt == null ? null : publishedAt.toInstant());
    }

    private RecruitmentSkillCard mapCard(ResultSet rs, int row) throws SQLException {
        return new RecruitmentSkillCard(rs.getLong("id"), rs.getString("name"), rs.getString("category"),
                rs.getString("description"), rs.getString("scenario_text"),
                split(rs.getString("trigger_keywords")), rs.getInt("priority"), rs.getString("current_version"));
    }

    private Long nullableLong(ResultSet rs, String name) throws SQLException {
        long value = rs.getLong(name);
        return rs.wasNull() ? null : value;
    }

    private String join(Set<String> values) {
        if (values == null || values.isEmpty()) return "";
        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .sorted(Comparator.naturalOrder())
                .collect(java.util.stream.Collectors.joining(","));
    }

    private Set<String> split(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
