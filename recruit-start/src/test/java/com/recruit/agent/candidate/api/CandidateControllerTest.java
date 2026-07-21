package com.recruit.agent.candidate.api;

import com.recruit.agent.candidate.application.CandidateApplicationService;
import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeParseStatus;
import com.recruit.agent.common.api.ApiExceptionHandler;
import com.recruit.agent.common.security.SecurityConfiguration;
import com.recruit.agent.common.storage.FileStorage;
import com.recruit.agent.common.storage.StoredFile;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CandidateController.class)
@Import({ApiExceptionHandler.class, SecurityConfiguration.class})
class CandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CandidateApplicationService service;

    @MockBean
    private FileStorage fileStorage;

    @Test
    void registersCandidate() throws Exception {
        when(service.register(any())).thenReturn(candidate());

        mockMvc.perform(post("/api/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "王五",
                                  "phoneHash": "phone-hash",
                                  "emailHash": "email-hash",
                                  "educationLevel": "BACHELOR",
                                  "totalExperienceYears": 2.5,
                                  "expectedCity": "成都",
                                  "skills": ["Java", "Spring Boot"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2001))
                .andExpect(jsonPath("$.name").value("王五"))
                .andExpect(jsonPath("$.skills", hasItems("java", "spring boot")));
    }

    @Test
    void rejectsBlankCandidateName() throws Exception {
        mockMvc.perform(post("/api/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "educationLevel": "BACHELOR",
                                  "totalExperienceYears": 2.5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("候选人姓名不能为空"));
    }

    @Test
    void addsResumeForCandidate() throws Exception {
        when(service.addResume(any())).thenReturn(resume());

        mockMvc.perform(post("/api/candidates/2001/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalFilename": "resume.pdf",
                                  "contentType": "application/pdf",
                                  "fileSize": 1024,
                                  "storageObjectName": "resumes/2026/resume.pdf"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3001))
                .andExpect(jsonPath("$.candidateId").value(2001))
                .andExpect(jsonPath("$.parseStatus").value("UPLOADED"));
    }

    @Test
    void returnsNotFoundWhenCandidateMissingWhileAddingResume() throws Exception {
        when(service.addResume(any())).thenThrow(new NoSuchElementException("候选人不存在: 999"));

        mockMvc.perform(post("/api/candidates/999/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalFilename": "resume.pdf",
                                  "contentType": "application/pdf",
                                  "fileSize": 1024,
                                  "storageObjectName": "resumes/missing.pdf"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("候选人不存在: 999"));
    }

    @Test
    void listsCandidateResumes() throws Exception {
        when(service.listResumes(2001L)).thenReturn(List.of(resume()));

        mockMvc.perform(get("/api/candidates/2001/resumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3001))
                .andExpect(jsonPath("$[0].originalFilename").value("resume.pdf"));
    }

    @Test
    void uploadsResumeFile() throws Exception {
        when(fileStorage.store(any())).thenReturn(new StoredFile(
                "resume.pdf",
                "application/pdf",
                1024,
                "2026/07/11/resume.pdf"
        ));
        when(service.addResume(any())).thenReturn(resume());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "resume-content".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/candidates/2001/resumes/upload")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3001))
                .andExpect(jsonPath("$.parseStatus").value("UPLOADED"));
    }

    @Test
    void rejectsEmptyResumeFile() throws Exception {
        mockMvc.perform(multipart("/api/candidates/2001/resumes/upload")
                        .file("file", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("简历文件不能为空"));
    }

    private Candidate candidate() {
        return new Candidate(
                2001L,
                "王五",
                "phone-hash",
                "email-hash",
                EducationLevel.BACHELOR,
                2.5,
                "成都",
                Set.of("java", "spring boot"),
                Instant.parse("2026-07-11T00:00:00Z")
        );
    }

    private ResumeDocument resume() {
        return new ResumeDocument(
                3001L,
                2001L,
                "resume.pdf",
                "application/pdf",
                1024,
                "resumes/2026/resume.pdf",
                ResumeParseStatus.UPLOADED,
                null,
                Instant.parse("2026-07-11T00:00:00Z")
        );
    }
}
