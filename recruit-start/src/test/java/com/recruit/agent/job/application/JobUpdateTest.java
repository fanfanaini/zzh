package com.recruit.agent.job.application;

import com.recruit.agent.job.infrastructure.InMemoryJobRepository;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JobUpdateTest {

    @Test
    void shouldUpdateExistingJobAndKeepIdentity() {
        JobApplicationService service = new JobApplicationService(new InMemoryJobRepository());
        var created = service.create(new CreateJobCommand("Java", "old", Set.of("java"), Set.of(),
                EducationLevel.BACHELOR, 1, 3));

        var updated = service.update(created.id(), new CreateJobCommand("Senior Java", "new", Set.of("java", "mysql"),
                Set.of("redis"), EducationLevel.BACHELOR, 3, 6));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Senior Java");
        assertThat(updated.createdAt()).isEqualTo(created.createdAt());
    }
}
