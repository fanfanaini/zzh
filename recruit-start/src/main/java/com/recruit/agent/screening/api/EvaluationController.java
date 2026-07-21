package com.recruit.agent.screening.api;

import com.recruit.agent.screening.application.EvaluationApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationApplicationService service;

    public EvaluationController(EvaluationApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public EvaluationResponse get(@PathVariable("id") Long id) {
        return EvaluationResponse.from(service.get(id));
    }

    @PostMapping("/{id}/human-review")
    @ResponseStatus(HttpStatus.OK)
    public EvaluationResponse humanReview(
            @PathVariable("id") Long id,
            @Valid @RequestBody HumanReviewRequest request
    ) {
        return EvaluationResponse.from(service.humanReview(id, request.humanResult()));
    }
}
