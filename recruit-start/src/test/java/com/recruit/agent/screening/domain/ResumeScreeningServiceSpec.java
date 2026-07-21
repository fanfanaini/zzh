package com.recruit.agent.screening.domain;

import java.util.Set;

public class ResumeScreeningServiceSpec {

    public static void main(String[] args) {
        shouldRejectWhenRequiredSkillMissing();
        shouldRecommendInterviewWhenCoreSignalsAreStrong();
        System.out.println("ResumeScreeningServiceSpec passed");
    }

    private static void shouldRejectWhenRequiredSkillMissing() {
        ResumeScreeningService service = new ResumeScreeningService();
        ScreeningResult result = service.screen(defaultJavaJob(), new CandidateProfile(
                1L,
                "张三",
                Set.of("java", "mysql"),
                EducationLevel.BACHELOR,
                2.0,
                Set.of("spring boot", "mysql")
        ));

        assertEquals(ScreeningRecommendation.NOT_MATCH, result.recommendation(), "缺少必须技能时应直接不匹配");
        assertTrue(result.risks().get(0).contains("spring boot"), "风险里应说明缺失的技能");
    }

    private static void shouldRecommendInterviewWhenCoreSignalsAreStrong() {
        ResumeScreeningService service = new ResumeScreeningService();
        ScreeningResult result = service.screen(defaultJavaJob(), new CandidateProfile(
                2L,
                "李四",
                Set.of("java", "spring boot", "mysql", "redis"),
                EducationLevel.BACHELOR,
                3.0,
                Set.of("java", "spring boot", "mysql")
        ));

        assertEquals(ScreeningRecommendation.INTERVIEW, result.recommendation(), "强匹配候选人应推荐面试");
        assertEquals(95.0, result.score().finalScore(), "最终分应按 README 中的权重公式计算");
        assertTrue(result.advantages().contains("必须技能匹配度高"), "应输出优势证据");
    }

    private static JobProfile defaultJavaJob() {
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

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ", expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
