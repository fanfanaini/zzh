package com.recruit.agent.screening.api;

import com.recruit.agent.common.api.ApiExceptionHandler;
import com.recruit.agent.common.security.SecurityConfiguration;
import com.recruit.agent.screening.application.ScreeningApplicationService;
import com.recruit.agent.screening.domain.ScoreBreakdown;
import com.recruit.agent.screening.domain.ScreeningRecommendation;
import com.recruit.agent.screening.domain.ScreeningResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScreeningController.class)
@Import({ApiExceptionHandler.class, SecurityConfiguration.class})
class ScreeningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScreeningApplicationService service;

    @Test
    void screensCandidate() throws Exception {
        when(service.screen(any())).thenReturn(interviewResult());

        mockMvc.perform(post("/api/jobs/1001/screen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateId": 2001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").value("INTERVIEW"))
                .andExpect(jsonPath("$.score.finalScore").value(95.0))
                .andExpect(jsonPath("$.advantages[0]").value("必须技能匹配度高"));
    }

    @Test
    void rejectsMissingCandidateId() throws Exception {
        mockMvc.perform(post("/api/jobs/1001/screen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("候选人 ID 不能为空"));
    }

    @Test
    void returnsNotFoundWhenJobMissing() throws Exception {
        when(service.screen(any())).thenThrow(new NoSuchElementException("岗位不存在: 404"));

        mockMvc.perform(post("/api/jobs/404/screen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateId": 2001
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("岗位不存在: 404"));
    }

    private ScreeningResult interviewResult() {
        return new ScreeningResult(
                ScreeningRecommendation.INTERVIEW,
                new ScoreBreakdown(100, 100, 100, 100, 50, 95),
                List.of("必须技能匹配度高"),
                List.of(),
                List.of()
        );
    }
}
