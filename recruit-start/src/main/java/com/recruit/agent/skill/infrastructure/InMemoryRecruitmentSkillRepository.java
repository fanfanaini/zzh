package com.recruit.agent.skill.infrastructure;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.skill.domain.RecruitmentSkill;
import com.recruit.agent.skill.domain.RecruitmentSkillCard;
import com.recruit.agent.skill.domain.RecruitmentSkillRepository;
import com.recruit.agent.skill.domain.RecruitmentSkillVersion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRecruitmentSkillRepository implements RecruitmentSkillRepository {
    private final Map<Long, RecruitmentSkill> skills = new ConcurrentHashMap<>();
    private final Map<Long, RecruitmentSkillVersion> versions = new ConcurrentHashMap<>();
    private final TimestampIdGenerator ids = new TimestampIdGenerator();

    @Override
    public RecruitmentSkill saveSkill(RecruitmentSkill skill) {
        skills.put(skill.id(), skill);
        return skill;
    }

    @Override
    public RecruitmentSkillVersion saveVersion(RecruitmentSkillVersion version) {
        versions.put(version.id(), version);
        return version;
    }

    @Override
    public void publish(Long skillId, RecruitmentSkillVersion version) {
        Instant now = Instant.now();
        versions.put(version.id(), version.publish(now));
        skills.computeIfPresent(skillId, (ignored, skill) -> skill.publish(version.id(), now));
    }

    @Override
    public Optional<RecruitmentSkill> findSkill(Long id) {
        return Optional.ofNullable(skills.get(id));
    }

    @Override
    public Optional<RecruitmentSkillVersion> findVersion(Long versionId) {
        return Optional.ofNullable(versions.get(versionId));
    }

    @Override
    public Optional<RecruitmentSkillVersion> findCurrentVersion(Long skillId) {
        return findSkill(skillId)
                .map(RecruitmentSkill::currentVersionId)
                .flatMap(this::findVersion);
    }

    @Override
    public List<RecruitmentSkillCard> enabledCards() {
        return skills.values().stream()
                .filter(RecruitmentSkill::enabled)
                .map(skill -> new RecruitmentSkillCard(skill.id(), skill.name(), skill.category(), skill.description(),
                        skill.scenario(), skill.triggerKeywords(), skill.priority(),
                        findVersion(skill.currentVersionId()).map(RecruitmentSkillVersion::version).orElse("")))
                .sorted(Comparator.comparingInt(RecruitmentSkillCard::priority).reversed())
                .toList();
    }

    @Override
    public List<RecruitmentSkill> listSkills() {
        return skills.values().stream()
                .sorted(Comparator.comparing(RecruitmentSkill::updatedAt).reversed())
                .toList();
    }

    @Override
    public List<RecruitmentSkillVersion> listVersions(Long skillId) {
        return versions.values().stream()
                .filter(version -> version.skillId().equals(skillId))
                .sorted(Comparator.comparing(RecruitmentSkillVersion::createdAt).reversed())
                .toList();
    }

    @Override
    public long nextId() {
        return ids.nextId();
    }

    public void seed(List<RecruitmentSkill> seededSkills, List<RecruitmentSkillVersion> seededVersions) {
        new ArrayList<>(seededSkills).forEach(skill -> skills.put(skill.id(), skill));
        new ArrayList<>(seededVersions).forEach(version -> versions.put(version.id(), version));
    }
}
