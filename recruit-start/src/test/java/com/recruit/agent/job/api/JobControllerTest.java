package com.recruit.agent.job.api;

import com.recruit.agent.common.api.ApiExceptionHandler;
import com.recruit.agent.common.security.SecurityConfiguration;
import com.recruit.agent.job.application.JobApplicationService;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import({ApiExceptionHandler.class, SecurityConfiguration.class})
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobApplicationService service;

    @Test
    void createsJob() throws Exception {
        when(service.create(any())).thenReturn(javaJob());

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Java后端开发工程师",
                                  "description": "负责招聘系统后端接口与AI任务编排",
                                  "requiredSkills": ["Java", "Spring Boot"],
                                  "preferredSkills": ["Redis"],
                                  "educationRequirement": "BACHELOR",
                                  "minExperienceYears": 1,
                                  "maxExperienceYears": 3
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1001))
                .andExpect(jsonPath("$.name").value("Java后端开发工程师"))
                .andExpect(jsonPath("$.requiredSkills", hasItems("java", "spring boot")));
    }

    @Test
    void rejectsBlankJobName() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "description": "负责招聘系统后端接口与AI任务编排",
                                  "minExperienceYears": 1,
                                  "maxExperienceYears": 3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("岗位名称不能为空"));
    }

    @Test
    void returnsNotFoundWhenJobMissing() throws Exception {
        when(service.get(404L)).thenThrow(new NoSuchElementException("岗位不存在: 404"));

        mockMvc.perform(get("/api/jobs/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("岗位不存在: 404"));
    }

    @Test
    void listsJobs() throws Exception {
        when(service.list()).thenReturn(List.of(javaJob()));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1001))
                .andExpect(jsonPath("$[0].preferredSkills[0]").value("redis"));
    }

    private Job javaJob() {
        return new Job(
                1001L,
                "Java后端开发工程师",
                "负责招聘系统后端接口与AI任务编排",
                Set.of("java", "spring boot"),
                Set.of("redis"),
                EducationLevel.BACHELOR,
                1,
                3,
                Instant.parse("2026-07-11T00:00:00Z")
        );
    }
}
