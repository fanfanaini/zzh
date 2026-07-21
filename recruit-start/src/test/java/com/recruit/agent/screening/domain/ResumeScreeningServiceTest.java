package com.recruit.agent.screening.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeScreeningServiceTest {

    private final ResumeScreeningService service = new ResumeScreeningService();

    @Test
    void rejectsCandidateWhenRequiredSkillIsMissing() {
        ScreeningResult result = service.screen(defaultJavaJob(), new CandidateProfile(
                1L,
                "张三",
                Set.of("java", "mysql"),
                EducationLevel.BACHELOR,
                2.0,
                Set.of("spring boot", "mysql")
        ));

        assertThat(result.recommendation()).isEqualTo(ScreeningRecommendation.NOT_MATCH);
        assertThat(result.risks()).anyMatch(risk -> risk.contains("spring boot"));
    }

    @Test
    void recommendsInterviewWhenCoreSignalsAreStrong() {
        ScreeningResult result = service.screen(defaultJavaJob(), new CandidateProfile(
                2L,
                "李四",
                Set.of("java", "spring boot", "mysql", "redis"),
                EducationLevel.BACHELOR,
                3.0,
                Set.of("java", "spring boot", "mysql")
        ));

        assertThat(result.recommendation()).isEqualTo(ScreeningRecommendation.INTERVIEW);
        assertThat(result.score().finalScore()).isEqualTo(95.0);
        assertThat(result.advantages()).contains("必须技能匹配度高");
    }

    private JobProfile defaultJavaJob() {
        return new JobProfile(
                100L,
                "Java后端开发工程师",
                Set.of("java", "spring boot", "mysql"),
                Set.of("redis", "rabbitmq"),
                EducationLevel.BACHELOR,
                1.0,
                Set.of("必须具有Java项目经验")
        );
    }
}
