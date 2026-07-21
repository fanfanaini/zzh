package com.recruit.agent.job.application;

import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.infrastructure.InMemoryJobRepository;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobApplicationServiceTest {

    @Test
    void createsAndFindsJob() {
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

        assertThat(service.get(created.id()).requiredSkills()).contains("spring boot");
        assertThat(service.list()).hasSize(1);
    }

    @Test
    void rejectsInvalidExperienceRange() {
        JobApplicationService service = new JobApplicationService(new InMemoryJobRepository());

        assertThatThrownBy(() -> service.create(new CreateJobCommand(
                "Java后端开发工程师",
                "负责招聘系统后端接口与AI任务编排",
                Set.of("Java"),
                Set.of(),
                EducationLevel.BACHELOR,
                5,
                3
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最低工作年限");
    }
}
