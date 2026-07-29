package com.recruit.agent.skill.application;

import com.recruit.agent.skill.domain.RecruitmentSkill;
import com.recruit.agent.skill.domain.RecruitmentSkillStatus;
import com.recruit.agent.skill.domain.RecruitmentSkillVersion;
import com.recruit.agent.skill.infrastructure.InMemoryRecruitmentSkillRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SkillContextAssemblerTest {

    @Test
    void loadsFullSkillOnlyAfterCardMatch() {
        InMemoryRecruitmentSkillRepository repository = new InMemoryRecruitmentSkillRepository();
        Instant now = Instant.parse("2026-07-28T00:00:00Z");
        RecruitmentSkill skill = new RecruitmentSkill(1L, "salary_expectation_reply", "conversation",
                "salary", "salary questions", Set.of("薪资"), 90,
                RecruitmentSkillStatus.ENABLED, 10L, now, now);
        RecruitmentSkillVersion version = new RecruitmentSkillVersion(10L, 1L, "v1",
                "Do not promise salary.", "Ask expectation.", "", "No invented numbers.",
                Set.of("get_job_info"), Set.of("salary_commitment"), "handoff to HR", true, now, now);
        repository.saveSkill(skill);
        repository.saveVersion(version);

        RecruitmentSkillProperties properties = new RecruitmentSkillProperties();
        SkillContextAssembler assembler = new SkillContextAssembler(repository, new RecruitmentSkillMatcher(),
                properties, null);

        SkillContext context = assembler.assemble("候选人问薪资是多少");

        assertThat(context.matches()).extracting(SkillMatch::skillId).containsExactly(1L);
        assertThat(context.promptContext()).contains("Do not promise salary.").contains("version: v1");
        assertThat(context.allowedTools()).contains("get_job_info");
        assertThat(context.riskTags()).contains("salary_commitment");
    }
}
