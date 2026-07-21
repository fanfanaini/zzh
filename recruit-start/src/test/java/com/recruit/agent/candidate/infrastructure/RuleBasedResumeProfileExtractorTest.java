package com.recruit.agent.candidate.infrastructure;

import com.recruit.agent.candidate.domain.CandidateProfileDraft;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedResumeProfileExtractorTest {

    private final RuleBasedResumeProfileExtractor extractor = new RuleBasedResumeProfileExtractor();

    @Test
    void extractsSkillsExperienceCityAndEducation() {
        CandidateProfileDraft draft = extractor.extract("""
                张三
                本科学历，成都，3年 Java 后端开发经验。
                熟悉 Spring Boot、MySQL、Redis、Docker。
                """);

        assertThat(draft.educationLevel()).isEqualTo(EducationLevel.BACHELOR);
        assertThat(draft.totalExperienceYears()).isEqualTo(3.0);
        assertThat(draft.expectedCity()).isEqualTo("成都");
        assertThat(draft.skills()).contains("java", "spring boot", "mysql", "redis", "docker");
    }

    @Test
    void mapsSkillAliases() {
        CandidateProfileDraft draft = extractor.extract("熟悉 SpringBoot 和 K8s，有 2 years experience，本科");

        assertThat(draft.skills()).contains("spring boot", "kubernetes");
        assertThat(draft.totalExperienceYears()).isEqualTo(2.0);
    }
}
