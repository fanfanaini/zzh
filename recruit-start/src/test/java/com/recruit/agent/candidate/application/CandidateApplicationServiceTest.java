package com.recruit.agent.candidate.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.ResumeParseStatus;
import com.recruit.agent.candidate.infrastructure.InMemoryCandidateRepository;
import com.recruit.agent.candidate.infrastructure.InMemoryResumeRepository;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateApplicationServiceTest {

    @Test
    void registersCandidateAndAddsResume() {
        CandidateApplicationService service = newService();
        Candidate candidate = service.register(new RegisterCandidateCommand(
                "王五",
                "phone-hash",
                "email-hash",
                EducationLevel.BACHELOR,
                2.5,
                "成都",
                Set.of("Java", "Spring Boot")
        ));

        var resume = service.addResume(new AddResumeCommand(
                candidate.id(),
                "resume.pdf",
                "application/pdf",
                1024,
                "resumes/2026/resume.pdf"
        ));

        assertThat(candidate.skills()).contains("spring boot");
        assertThat(resume.parseStatus()).isEqualTo(ResumeParseStatus.UPLOADED);
        assertThat(service.listResumes(candidate.id())).hasSize(1);
    }

    @Test
    void rejectsResumeWhenCandidateMissing() {
        CandidateApplicationService service = newService();

        assertThatThrownBy(() -> service.addResume(new AddResumeCommand(
                999L,
                "resume.pdf",
                "application/pdf",
                1024,
                "resumes/missing.pdf"
        ))).isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("候选人不存在");
    }

    private CandidateApplicationService newService() {
        return new CandidateApplicationService(new InMemoryCandidateRepository(), new InMemoryResumeRepository());
    }
}
