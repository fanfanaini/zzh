package com.recruit.agent.skill.application;

import com.recruit.agent.skill.domain.RecruitmentSkill;
import com.recruit.agent.skill.domain.RecruitmentSkillCard;
import com.recruit.agent.skill.domain.RecruitmentSkillRepository;
import com.recruit.agent.skill.domain.RecruitmentSkillStatus;
import com.recruit.agent.skill.domain.RecruitmentSkillVersion;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

public class RecruitmentSkillApplicationService {
    private final RecruitmentSkillRepository repository;
    private final RecruitmentSkillMatcher matcher;
    private final RecruitmentSkillProperties properties;

    public RecruitmentSkillApplicationService(RecruitmentSkillRepository repository,
                                              RecruitmentSkillMatcher matcher,
                                              RecruitmentSkillProperties properties) {
        this.repository = repository;
        this.matcher = matcher;
        this.properties = properties;
    }

    public RecruitmentSkill create(CreateRecruitmentSkillCommand command) {
        Instant now = Instant.now();
        RecruitmentSkill skill = repository.saveSkill(new RecruitmentSkill(
                repository.nextId(), command.name(), command.category(), command.description(), command.scenario(),
                command.triggerKeywords(), command.priority(), RecruitmentSkillStatus.DRAFT, null, now, now));
        RecruitmentSkillVersion version = addVersion(skill.id(), new CreateRecruitmentSkillVersionCommand(
                command.version(), command.instruction(), command.promptFragment(), command.fewShots(),
                command.guardrails(), command.allowedTools(), command.riskTags(), command.handoffPolicy(),
                command.publish()));
        return command.publish() ? repository.findSkill(skill.id()).orElse(skill.publish(version.id(), now)) : skill;
    }

    public RecruitmentSkillVersion addVersion(Long skillId, CreateRecruitmentSkillVersionCommand command) {
        requireSkill(skillId);
        Instant now = Instant.now();
        RecruitmentSkillVersion version = repository.saveVersion(new RecruitmentSkillVersion(
                repository.nextId(), skillId, command.version(), command.instruction(), command.promptFragment(),
                command.fewShots(), command.guardrails(), command.allowedTools(), command.riskTags(),
                command.handoffPolicy(), false, now, null));
        if (command.publish()) {
            publish(skillId, version.id());
            return repository.findVersion(version.id()).orElse(version.publish(now));
        }
        return version;
    }

    public RecruitmentSkill publish(Long skillId, Long versionId) {
        RecruitmentSkill skill = requireSkill(skillId);
        RecruitmentSkillVersion version = repository.findVersion(versionId)
                .filter(candidate -> candidate.skillId().equals(skill.id()))
                .orElseThrow(() -> new NoSuchElementException("Skill version does not exist: " + versionId));
        repository.publish(skillId, version);
        return repository.findSkill(skillId).orElse(skill.publish(version.id(), Instant.now()));
    }

    public RecruitmentSkill disable(Long skillId) {
        RecruitmentSkill skill = requireSkill(skillId);
        RecruitmentSkill disabled = skill.disable(Instant.now());
        return repository.saveSkill(disabled);
    }

    public List<RecruitmentSkill> list() {
        return repository.listSkills();
    }

    public List<RecruitmentSkillCard> cards() {
        return repository.enabledCards();
    }

    public RecruitmentSkill detail(Long id) {
        return requireSkill(id);
    }

    public List<RecruitmentSkillVersion> versions(Long skillId) {
        requireSkill(skillId);
        return repository.listVersions(skillId);
    }

    public List<SkillMatch> match(String query, Integer limit) {
        return matcher.match(query, repository.enabledCards(),
                limit == null ? properties.getMatchLimit() : limit, properties.getMinScore());
    }

    private RecruitmentSkill requireSkill(Long id) {
        return repository.findSkill(id).orElseThrow(() -> new NoSuchElementException("Skill does not exist: " + id));
    }
}
