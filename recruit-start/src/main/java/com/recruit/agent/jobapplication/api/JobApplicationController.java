package com.recruit.agent.jobapplication.api;

import com.recruit.agent.common.api.PageResponse;
import com.recruit.agent.jobapplication.application.AdvanceJobApplicationCommand;
import com.recruit.agent.jobapplication.application.CreateJobApplicationCommand;
import com.recruit.agent.jobapplication.application.JobApplicationPipelineService;
import com.recruit.agent.jobapplication.domain.JobApplicationStage;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/job-applications")
public class JobApplicationController {

    private final JobApplicationPipelineService service;

    public JobApplicationController(JobApplicationPipelineService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobApplicationResponse create(@Valid @RequestBody CreateJobApplicationRequest request) {
        return JobApplicationResponse.from(service.create(new CreateJobApplicationCommand(
                request.jobId(),
                request.candidateId(),
                request.ownerUserId(),
                request.source(),
                request.note()
        )));
    }

    @GetMapping("/{id}")
    public JobApplicationResponse get(@PathVariable("id") Long id) {
        return JobApplicationResponse.from(service.get(id));
    }

    @GetMapping
    public Object list(@RequestParam(value = "jobId", required = false) Long jobId,
                       @RequestParam(value = "candidateId", required = false) Long candidateId,
                       @RequestParam(value = "stage", required = false) JobApplicationStage stage,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size) {
        List<JobApplicationResponse> applications = service.list(jobId, candidateId, stage).stream()
                .map(JobApplicationResponse::from)
                .toList();
        if (page != null || size != null) {
            return PageResponse.of(applications, page == null ? 1 : page, size == null ? 20 : size);
        }
        return applications;
    }

    @PatchMapping("/{id}/stage")
    public JobApplicationResponse advance(@PathVariable("id") Long id,
                                          @Valid @RequestBody AdvanceJobApplicationRequest request) {
        return JobApplicationResponse.from(service.advance(new AdvanceJobApplicationCommand(
                id,
                request.stage(),
                request.operatorUserId(),
                request.reason(),
                request.evaluationId(),
                request.conversationId(),
                request.note()
        )));
    }

    @GetMapping("/{id}/history")
    public List<JobApplicationHistoryResponse> history(@PathVariable("id") Long id) {
        return service.history(id).stream()
                .map(JobApplicationHistoryResponse::from)
                .toList();
    }
}
