package com.recruit.agent.evaluation.application;

import com.recruit.agent.evaluation.domain.EvaluationReport;

import java.io.IOException;
import java.nio.file.Path;

public class OfflineEvaluationRunner {

    private final EvaluationJsonlParser parser;
    private final AgentEvaluationService evaluationService;

    public OfflineEvaluationRunner(EvaluationJsonlParser parser, AgentEvaluationService evaluationService) {
        this.parser = parser;
        this.evaluationService = evaluationService;
    }

    public EvaluationReport run(Path casesPath, Path resultsPath, int retrievalK) throws IOException {
        return evaluationService.evaluate(parser.parseCases(casesPath), parser.parseResults(resultsPath), retrievalK);
    }
}
