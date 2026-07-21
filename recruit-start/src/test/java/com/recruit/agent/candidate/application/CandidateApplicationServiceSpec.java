package com.recruit.agent.candidate.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeParseStatus;
import com.recruit.agent.candidate.infrastructure.InMemoryCandidateRepository;
import com.recruit.agent.candidate.infrastructure.InMemoryResumeRepository;
import com.recruit.agent.screening.domain.EducationLevel;

import java.util.Set;

public class CandidateApplicationServiceSpec {

    public static void main(String[] args) {
        shouldRegisterCandidate();
        shouldAddResumeForExistingCandidate();
        shouldRejectResumeWhenCandidateMissing();
        System.out.println("CandidateApplicationServiceSpec passed");
    }

    private static void shouldRegisterCandidate() {
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

        assertEquals(candidate.id(), service.get(candidate.id()).id(), "注册后应能按 ID 查询候选人");
        assertTrue(candidate.skills().contains("spring boot"), "技能应标准化为小写");
    }

    private static void shouldAddResumeForExistingCandidate() {
        CandidateApplicationService service = newService();
        Candidate candidate = service.register(new RegisterCandidateCommand(
                "赵六",
                "phone-hash",
                "email-hash",
                EducationLevel.BACHELOR,
                1.0,
                "上海",
                Set.of("Java")
        ));

        ResumeDocument resume = service.addResume(new AddResumeCommand(
                candidate.id(),
                "resume.pdf",
                "application/pdf",
                1024,
                "resumes/2026/resume.pdf"
        ));

        assertEquals(ResumeParseStatus.UPLOADED, resume.parseStatus(), "新增简历初始状态应为 UPLOADED");
        assertEquals(1, service.listResumes(candidate.id()).size(), "候选人应能查询到自己的简历");
    }

    private static void shouldRejectResumeWhenCandidateMissing() {
        CandidateApplicationService service = newService();
        try {
            service.addResume(new AddResumeCommand(
                    999L,
                    "resume.pdf",
                    "application/pdf",
                    1024,
                    "resumes/missing.pdf"
            ));
            throw new AssertionError("候选人不存在时不应允许添加简历");
        } catch (java.util.NoSuchElementException expected) {
            assertTrue(expected.getMessage().contains("候选人不存在"), "异常信息应说明候选人不存在");
        }
    }

    private static CandidateApplicationService newService() {
        return new CandidateApplicationService(new InMemoryCandidateRepository(), new InMemoryResumeRepository());
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
