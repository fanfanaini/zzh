package com.recruit.agent.ai.api;

import com.recruit.agent.ai.application.ModelCallMonitoringService;
import com.recruit.agent.ai.domain.ModelCallLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/model-calls")
public class ModelCallController {
    private final ModelCallMonitoringService service;
    public ModelCallController(ModelCallMonitoringService service) { this.service = service; }

    @GetMapping
    public List<ModelCallLog> list() { return service.list(); }

    @GetMapping("/summary")
    public ModelCallMonitoringService.ModelCallSummary summary() { return service.summary(); }
}
