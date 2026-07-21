package com.recruit.agent.job.application;

import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.infrastructure.InMemoryJobRepository;
import com.recruit.agent.screening.domain.EducationLevel;

import java.util.Set;

public class JobApplicationServiceSpec {

    public static void main(String[] args) {
        shouldCreateAndFindJob();
        shouldRejectInvalidExperienceRange();
        System.out.println("JobApplicationServiceSpec passed");
    }

    private static void shouldCreateAndFindJob() {
        JobApplicationService service = new JobApplicationService(new InMemoryJobRepository());

        Job created = service.create(new CreateJobCommand(
                "Java后端开发工程师",
                "负责招聘系统后端接口与AI任务编排",
                Set.of("Java", "Spring Boot", "MySQL"),
                Set.of("Redis"),
                EducationLevel.BACHELOR,
                1,
                3
        ));

        Job found = service.get(created.id());
        assertEquals(created.id(), found.id(), "创建后应能按 ID 查询岗位");
        assertTrue(found.requiredSkills().contains("spring boot"), "技能应标准化为小写");
    }

    private static void shouldRejectInvalidExperienceRange() {
        JobApplicationService service = new JobApplicationService(new InMemoryJobRepository());

        try {
            service.create(new CreateJobCommand(
                    "Java后端开发工程师",
                    "负责招聘系统后端接口与AI任务编排",
                    Set.of("Java"),
                    Set.of(),
                    EducationLevel.BACHELOR,
                    5,
                    3
            ));
            throw new AssertionError("最低工作年限大于最高工作年限时应失败");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("最低工作年限"), "异常信息应说明原因");
        }
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
