package com.recruit.agent.job.api;

import com.recruit.agent.job.application.CreateJobCommand;
import com.recruit.agent.job.application.JobApplicationService;
import com.recruit.agent.job.application.JobAnalysisService;
import com.recruit.agent.common.api.PageResponse;
import com.recruit.agent.job.domain.JobStatus;
import org.springframework.beans.factory.ObjectProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobApplicationService service;
    private final JobAnalysisService analysisService;

    public JobController(JobApplicationService service, ObjectProvider<JobAnalysisService> analysisService) {
        this.service = service;
        this.analysisService = analysisService.getIfAvailable();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse create(@Valid @RequestBody CreateJobRequest request) {
        return JobResponse.from(service.create(new CreateJobCommand(
                request.name(),
                request.description(),
                request.requiredSkills(),
                request.preferredSkills(),
                request.educationRequirement(),
                request.minExperienceYears(),
                request.maxExperienceYears()
        )));
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable("id") Long id) {
        return JobResponse.from(service.get(id));
    }

    @GetMapping
    public Object list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "status", required = false) JobStatus status,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size) {
        boolean hasFilter = keyword != null || status != null;
        List<JobResponse> jobs = (hasFilter ? service.search(keyword, status) : service.list()).stream()
                .map(JobResponse::from)
                .toList();
        if (page != null || size != null) {
            return PageResponse.of(jobs, page == null ? 1 : page, size == null ? 20 : size);
        }
        return jobs;
    }

    @PutMapping("/{id}")
    public JobResponse update(@PathVariable("id") Long id, @Valid @RequestBody CreateJobRequest request) {
        return JobResponse.from(service.update(id, new CreateJobCommand(
                request.name(), request.description(), request.requiredSkills(), request.preferredSkills(),
                request.educationRequirement(), request.minExperienceYears(), request.maxExperienceYears())));
    }

    @PostMapping("/{id}/analyze")
    public JobResponse analyze(@PathVariable("id") Long id) {
        if (analysisService == null) throw new IllegalStateException("岗位分析服务未启用");
        return JobResponse.from(analysisService.analyze(id));
    }

    @PatchMapping("/{id}/close")
    public JobResponse close(@PathVariable("id") Long id) {
        return JobResponse.from(service.close(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }
}
