package com.recruit.agent.evaluation.api;

import com.recruit.agent.evaluation.application.AgentCoreBaselineService;
import com.recruit.agent.evaluation.domain.EvaluationReport;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/evaluations")
public class AgentEvaluationController {
    private final AgentCoreBaselineService baselineService;
    private final ResourceLoader resourceLoader;

    public AgentEvaluationController(AgentCoreBaselineService baselineService, ResourceLoader resourceLoader) {
        this.baselineService = baselineService;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/core-baseline")
    public EvaluationReport coreBaseline() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + AgentCoreBaselineService.DATASET_RESOURCE);
        try (InputStream input = resource.getInputStream()) {
            return baselineService.run(input);
        }
    }
}
