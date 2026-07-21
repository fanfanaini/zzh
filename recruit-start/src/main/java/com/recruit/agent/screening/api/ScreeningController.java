package com.recruit.agent.screening.api;

import com.recruit.agent.screening.application.ScreenCandidateCommand;
import com.recruit.agent.screening.application.ScreeningApplicationService;
import com.recruit.agent.screening.application.BatchScreeningApplicationService;
import com.recruit.agent.common.api.PageResponse;
import com.recruit.agent.screening.domain.ScreeningRecommendation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.ObjectProvider;

@RestController
@RequestMapping("/api/jobs")
public class ScreeningController {

    private final ScreeningApplicationService service;
    private final BatchScreeningApplicationService batchService;

    public ScreeningController(ScreeningApplicationService service, ObjectProvider<BatchScreeningApplicationService> batchService) {
        this.service = service;
        this.batchService = batchService.getIfAvailable();
    }

    @PostMapping("/{jobId}/screen")
    @ResponseStatus(HttpStatus.OK)
    public ScreeningResultResponse screen(
            @PathVariable("jobId") Long jobId,
            @Valid @RequestBody ScreenCandidateRequest request
    ) {
        return ScreeningResultResponse.from(service.screen(new ScreenCandidateCommand(jobId, request.candidateId())));
    }

    @PostMapping("/{jobId}/batch-screen")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public com.recruit.agent.task.api.AgentTaskResponse batchScreen(
            @PathVariable("jobId") Long jobId,
            @Valid @RequestBody BatchScreenRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        if (batchService == null) {
            throw new IllegalStateException("批量筛选服务未启用");
        }
        return com.recruit.agent.task.api.AgentTaskResponse.from(
                batchService.submit(jobId, request.candidateIds(), idempotencyKey));
    }

    @GetMapping("/{jobId}/candidates")
    public Object rankings(@PathVariable("jobId") Long jobId,
                           @org.springframework.web.bind.annotation.RequestParam(value = "candidateId", required = false) Long candidateId,
                           @org.springframework.web.bind.annotation.RequestParam(value = "keyword", required = false) String keyword,
                           @org.springframework.web.bind.annotation.RequestParam(value = "recommendation", required = false) ScreeningRecommendation recommendation,
                           @org.springframework.web.bind.annotation.RequestParam(value = "page", required = false) Integer page,
                           @org.springframework.web.bind.annotation.RequestParam(value = "size", required = false) Integer size) {
        java.util.List<CandidateRankingResponse> rankings = service.rankings(jobId, candidateId, keyword, recommendation)
                .stream().map(CandidateRankingResponse::from).toList();
        if (page != null || size != null) {
            return PageResponse.of(rankings, page == null ? 1 : page, size == null ? 20 : size);
        }
        return rankings;
    }
}
