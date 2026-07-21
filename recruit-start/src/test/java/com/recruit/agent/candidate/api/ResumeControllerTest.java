package com.recruit.agent.candidate.api;

import com.recruit.agent.candidate.application.CandidateProfileExtractionService;
import com.recruit.agent.candidate.application.CandidateResumeTextExtractionService;
import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeParseStatus;
import com.recruit.agent.common.api.ApiExceptionHandler;
import com.recruit.agent.common.document.TextExtractionException;
import com.recruit.agent.common.security.SecurityConfiguration;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeController.class)
@Import({ApiExceptionHandler.class, SecurityConfiguration.class})
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CandidateResumeTextExtractionService service;

    @MockBean
    private CandidateProfileExtractionService profileExtractionService;

    @Test
    void extractsResumeText() throws Exception {
        when(service.extractText(3001L)).thenReturn(parsedResume());

        mockMvc.perform(post("/api/resumes/3001/extract-text"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3001))
                .andExpect(jsonPath("$.parseStatus").value("PARSED"));
    }

    @Test
    void returnsNotFoundWhenResumeMissing() throws Exception {
        when(service.extractText(404L)).thenThrow(new NoSuchElementException("简历不存在: 404"));

        mockMvc.perform(post("/api/resumes/404/extract-text"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("简历不存在: 404"));
    }

    @Test
    void returnsBadRequestWhenDocumentTypeUnsupported() throws Exception {
        when(service.extractText(3002L)).thenThrow(new TextExtractionException("暂不支持该简历文件类型"));

        mockMvc.perform(post("/api/resumes/3002/extract-text"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("暂不支持该简历文件类型"));
    }

    @Test
    void extractsCandidateProfile() throws Exception {
        when(profileExtractionService.extractProfile(3001L)).thenReturn(candidate());

        mockMvc.perform(post("/api/resumes/3001/extract-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2001))
                .andExpect(jsonPath("$.educationLevel").value("BACHELOR"))
                .andExpect(jsonPath("$.skills").isArray());
    }

    @Test
    void returnsConflictWhenRawTextMissing() throws Exception {
        when(profileExtractionService.extractProfile(3001L))
                .thenThrow(new IllegalStateException("简历尚未完成文本提取"));

        mockMvc.perform(post("/api/resumes/3001/extract-profile"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("简历尚未完成文本提取"));
    }

    private ResumeDocument parsedResume() {
        return new ResumeDocument(
                3001L,
                2001L,
                "resume.txt",
                "text/plain",
                16,
                "2026/07/11/resume.txt",
                ResumeParseStatus.PARSED,
                "Java Spring Boot",
                Instant.parse("2026-07-11T00:00:00Z")
        );
    }

    private Candidate candidate() {
        return new Candidate(
                2001L,
                "张三",
                "phone-hash",
                "email-hash",
                EducationLevel.BACHELOR,
                3.0,
                "成都",
                Set.of("java", "spring boot"),
                Instant.parse("2026-07-11T00:00:00Z")
        );
    }
}
