package com.recruit.agent.skill.domain;

import java.util.List;
import java.util.Optional;

public interface RecruitmentSkillRepository {
    RecruitmentSkill saveSkill(RecruitmentSkill skill);
    RecruitmentSkillVersion saveVersion(RecruitmentSkillVersion version);
    void publish(Long skillId, RecruitmentSkillVersion version);
    Optional<RecruitmentSkill> findSkill(Long id);
    Optional<RecruitmentSkillVersion> findVersion(Long versionId);
    Optional<RecruitmentSkillVersion> findCurrentVersion(Long skillId);
    List<RecruitmentSkillCard> enabledCards();
    List<RecruitmentSkill> listSkills();
    List<RecruitmentSkillVersion> listVersions(Long skillId);
    long nextId();
}
