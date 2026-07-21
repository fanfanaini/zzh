package com.recruit.agent.vector.api;

import com.recruit.agent.job.application.JobApplicationService;
import com.recruit.agent.vector.application.VectorApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/vector")
public class VectorController {

    private final VectorApplicationService vectorService;
    private final JobApplicationService jobService;

    public VectorController(VectorApplicationService vectorService, JobApplicationService jobService) {
        this.vectorService = vectorService;
        this.jobService = jobService;
    }

    @PostMapping("/jobs/{jobId}/description")
    @ResponseStatus(HttpStatus.CREATED)
    public void indexJobDescription(
            @PathVariable("jobId") Long jobId,
            @Valid @RequestBody IndexJobDescriptionRequest request
    ) {
        vectorService.indexJobDescription(jobId, request.content());
    }

    @PostMapping("/candidates/{candidateId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public void indexResumeProject(
            @PathVariable("candidateId") Long candidateId,
            @Valid @RequestBody IndexResumeProjectRequest request
    ) {
        vectorService.indexResumeProject(candidateId, request.projectId(), request.content());
    }

    @GetMapping("/jobs/{jobId}/candidates/{candidateId}/similarity")
    public ProjectSimilarityResponse similarity(
            @PathVariable("jobId") Long jobId,
            @PathVariable("candidateId") Long candidateId
    ) {
        String queryText = jobService.get(jobId).description();
        return vectorService.findBestProjectSimilarity(jobId, candidateId, queryText)
                .map(ProjectSimilarityResponse::from)
                .orElseThrow(() -> new NoSuchElementException("未找到候选人的项目向量: " + candidateId));
    }

}
