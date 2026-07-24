package com.recruit.agent.evaluation.api;

import com.recruit.agent.evaluation.application.AgentCoreBaselineService;
import com.recruit.agent.evaluation.domain.EvaluationReport;
import com.recruit.agent.evaluation.importing.AnonymizedConversationJsonlImporter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/api/evaluations")
public class AgentEvaluationController {
    private final AgentCoreBaselineService baselineService;
    private final ResourceLoader resourceLoader;
    private final AnonymizedConversationJsonlImporter reviewedConversationImporter;

    public AgentEvaluationController(
            AgentCoreBaselineService baselineService,
            ResourceLoader resourceLoader,
            AnonymizedConversationJsonlImporter reviewedConversationImporter
    ) {
        this.baselineService = baselineService;
        this.resourceLoader = resourceLoader;
        this.reviewedConversationImporter = reviewedConversationImporter;
    }

    @GetMapping("/core-baseline")
    public EvaluationReport coreBaseline() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + AgentCoreBaselineService.DATASET_RESOURCE);
        try (InputStream input = resource.getInputStream()) {
            return baselineService.run(input);
        }
    }

    @PostMapping(value = "/reviewed-baseline", consumes = "application/x-ndjson")
    public EvaluationReport reviewedBaseline(@RequestBody byte[] jsonl) throws IOException {
        if (jsonl == null || jsonl.length == 0) {
            throw new IllegalArgumentException("Evaluation JSONL must not be empty");
        }
        if (jsonl.length > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Evaluation JSONL must not exceed 5 MB");
        }
        try (InputStream input = new ByteArrayInputStream(jsonl)) {
            return baselineService.runCases(reviewedConversationImporter.importCases(input));
        }
    }
}
