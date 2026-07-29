package com.recruit.agent.memory.api;

import com.recruit.agent.memory.application.CandidateMemoryApplicationService;
import com.recruit.agent.memory.application.CreateCandidateMemoryCommand;
import com.recruit.agent.memory.domain.CandidateMemorySourceType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidates/{candidateId}/memories")
public class CandidateMemoryController {
    private final CandidateMemoryApplicationService service;

    public CandidateMemoryController(CandidateMemoryApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<CandidateMemoryResponse> list(@PathVariable Long candidateId,
                                              @RequestParam(value = "includeInactive", defaultValue = "false")
                                              boolean includeInactive) {
        return (includeInactive ? service.listAll(candidateId) : service.listActive(candidateId)).stream()
                .map(CandidateMemoryResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CandidateMemoryResponse create(@PathVariable Long candidateId,
                                          @Valid @RequestBody CreateCandidateMemoryRequest request) {
        return CandidateMemoryResponse.from(service.create(new CreateCandidateMemoryCommand(
                candidateId,
                request.type(),
                request.factKey(),
                request.factValue(),
                request.confidence(),
                CandidateMemorySourceType.MANUAL,
                null,
                request.evidence()
        )));
    }

    @PostMapping("/{memoryId}/deactivate")
    public CandidateMemoryResponse deactivate(@PathVariable Long candidateId, @PathVariable Long memoryId) {
        return CandidateMemoryResponse.from(service.deactivate(candidateId, memoryId));
    }
}
