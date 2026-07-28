package com.recruit.agent.evaluation.api;

import com.recruit.agent.evaluation.application.AgentCoreBaselineService;
import com.recruit.agent.evaluation.domain.EvaluationReport;
import com.recruit.agent.evaluation.importing.AnonymizedConversationJsonlImporter;
import com.recruit.agent.evaluation.application.RetrievalComparisonService;
import com.recruit.agent.evaluation.domain.RetrievalComparisonReport;
import com.recruit.agent.evaluation.domain.RetrievalQualityReport;
import com.recruit.agent.evaluation.domain.RetrievalRankingSample;
import com.recruit.agent.evaluation.application.RetrievalQualityReportService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class AgentEvaluationController {
    private final AgentCoreBaselineService baselineService;
    private final ResourceLoader resourceLoader;
    private final AnonymizedConversationJsonlImporter reviewedConversationImporter;
    private final RetrievalComparisonService retrievalComparisonService;
    private final RetrievalQualityReportService retrievalQualityReportService;

    public AgentEvaluationController(
            AgentCoreBaselineService baselineService,
            ResourceLoader resourceLoader,
            AnonymizedConversationJsonlImporter reviewedConversationImporter,
            RetrievalComparisonService retrievalComparisonService,
            RetrievalQualityReportService retrievalQualityReportService
    ) {
        this.baselineService = baselineService;
        this.resourceLoader = resourceLoader;
        this.reviewedConversationImporter = reviewedConversationImporter;
        this.retrievalComparisonService = retrievalComparisonService;
        this.retrievalQualityReportService = retrievalQualityReportService;
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

    @PostMapping("/retrieval-comparison")
    public RetrievalComparisonReport compareRetrieval(
            @RequestBody List<RetrievalRankingSample> samples,
            @RequestParam(defaultValue = "5") int k
    ) {
        return retrievalComparisonService.compare(samples, k);
    }

    @GetMapping("/retrieval-quality-report")
    public RetrievalQualityReport retrievalQualityReport(@RequestParam(defaultValue = "5") int k)
            throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + RetrievalQualityReportService.DATASET_RESOURCE);
        try (InputStream input = resource.getInputStream()) {
            return retrievalQualityReportService.run(input, k);
        }
    }
}
