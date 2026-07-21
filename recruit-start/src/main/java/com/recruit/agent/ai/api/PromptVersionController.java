package com.recruit.agent.ai.api;

import com.recruit.agent.ai.application.PromptApplicationService;
import com.recruit.agent.ai.domain.PromptVersion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
public class PromptVersionController {
    private final PromptApplicationService service;
    public PromptVersionController(PromptApplicationService service) { this.service = service; }

    @PostMapping("/{code}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public PromptVersion create(@PathVariable("code") String code, @Valid @RequestBody CreatePromptVersionRequest request) {
        return service.create(code, request.version(), request.systemPrompt(), request.userPromptTemplate(), request.enabled());
    }

    @GetMapping("/{code}/versions")
    public List<PromptVersion> list(@PathVariable("code") String code) { return service.list(code); }

    @GetMapping("/{code}/active")
    public PromptVersion active(@PathVariable("code") String code) { return service.enabled(code); }
}
