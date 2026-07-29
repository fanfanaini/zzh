package com.recruit.agent.skill.application;

import com.recruit.agent.skill.domain.RecruitmentSkillCard;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecruitmentSkillMatcherTest {

    @Test
    void matchesSkillCardsByKeywordsAndPriority() {
        RecruitmentSkillMatcher matcher = new RecruitmentSkillMatcher();

        List<SkillMatch> matches = matcher.match("候选人问这个岗位薪资待遇和 offer",
                List.of(
                        new RecruitmentSkillCard(1L, "salary_expectation_reply", "conversation",
                                "salary discussion", "salary and offer questions",
                                Set.of("薪资", "待遇", "offer"), 90, "v1"),
                        new RecruitmentSkillCard(2L, "interview_invitation", "conversation",
                                "interview scheduling", "schedule interviews",
                                Set.of("面试", "时间"), 80, "v1")
                ), 2, 20);

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).skillId()).isEqualTo(1L);
        assertThat(matches.get(0).score()).isGreaterThanOrEqualTo(20);
    }
}
