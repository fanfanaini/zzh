package com.recruit.agent.screening.domain;

import java.time.Instant;
import java.util.List;

public record Evaluation(
        Long id,
        Long jobId,
        Long candidateId,
        Double ruleScore,
        Double semanticScore,
        Double finalScore,
        ScreeningRecommendation recommendation,
        Double confidence,
        List<String> advantages,
        List<String> risks,
        List<String> missingInformation,
        String modelName,
        String promptVersion,
        String humanResult,
        Instant createdAt
) {

    public Evaluation {
        if (id == null) {
            throw new IllegalArgumentException("评估 ID 不能为空");
        }
        if (jobId == null) {
            throw new IllegalArgumentException("岗位 ID 不能为空");
        }
        if (candidateId == null) {
            throw new IllegalArgumentException("候选人 ID 不能为空");
        }
        if (recommendation == null) {
            throw new IllegalArgumentException("筛选建议不能为空");
        }
        advantages = normalize(advantages);
        risks = normalize(risks);
        missingInformation = normalize(missingInformation);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public static Evaluation fromScreeningResult(Long id, Long jobId, Long candidateId, ScreeningResult result) {
        double finalScore = result.score().finalScore();
        return new Evaluation(
                id,
                jobId,
                candidateId,
                finalScore,
                result.score().projectScore(),
                finalScore,
                result.recommendation(),
                Math.max(0, Math.min(1, finalScore / 100)),
                result.advantages(),
                result.risks(),
                result.missingInformation(),
                null,
                null,
                null,
                Instant.now()
        );
    }

    private static List<String> normalize(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    public Evaluation withHumanResult(String humanResult) {
        if (humanResult == null || humanResult.isBlank()) {
            throw new IllegalArgumentException("人工复核结果不能为空");
        }
        return new Evaluation(
                id,
                jobId,
                candidateId,
                ruleScore,
                semanticScore,
                finalScore,
                recommendation,
                confidence,
                advantages,
                risks,
                missingInformation,
                modelName,
                promptVersion,
                humanResult.trim().toUpperCase(),
                createdAt
        );
    }
}
