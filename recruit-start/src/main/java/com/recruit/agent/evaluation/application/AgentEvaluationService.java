package com.recruit.agent.evaluation.application;

import com.recruit.agent.evaluation.domain.BinaryClassificationMetrics;
import com.recruit.agent.evaluation.domain.ClassificationMetrics;
import com.recruit.agent.evaluation.domain.EndToEndMetrics;
import com.recruit.agent.evaluation.domain.EvaluationCase;
import com.recruit.agent.evaluation.domain.EvaluationReport;
import com.recruit.agent.evaluation.domain.EvaluationResult;
import com.recruit.agent.evaluation.domain.ExactMatchMetrics;
import com.recruit.agent.evaluation.domain.RetrievalMetrics;
import com.recruit.agent.evaluation.domain.RuntimeMetrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class AgentEvaluationService {

    public EvaluationReport evaluate(List<EvaluationCase> cases, List<EvaluationResult> results, int retrievalK) {
        if (cases == null || cases.isEmpty()) {
            throw new IllegalArgumentException("cases must not be empty");
        }
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("results must not be empty");
        }
        if (retrievalK <= 0) {
            throw new IllegalArgumentException("retrievalK must be positive");
        }

        String datasetVersion = requireSingleDatasetVersion(cases, results);
        Map<String, EvaluationResult> resultsByCaseId = indexResults(results);
        ensureExactCaseCoverage(cases, resultsByCaseId);
        return new EvaluationReport(
                datasetVersion,
                classification(cases, resultsByCaseId),
                exactMatch(cases, resultsByCaseId, EvaluationCase::expectedRoute, EvaluationResult::selectedRoute),
                exactMatch(cases, resultsByCaseId, EvaluationCase::expectedTool, EvaluationResult::selectedTool),
                retrieval(cases, resultsByCaseId, retrievalK),
                endToEnd(cases, resultsByCaseId),
                handoff(cases, resultsByCaseId),
                runtime(results),
                failureCounts(results)
        );
    }

    private ClassificationMetrics classification(List<EvaluationCase> cases,
                                                   Map<String, EvaluationResult> results) {
        long correct = 0;
        Set<String> labels = new HashSet<>();
        Map<String, Long> truePositive = new HashMap<>();
        Map<String, Long> falsePositive = new HashMap<>();
        Map<String, Long> falseNegative = new HashMap<>();
        for (EvaluationCase evaluationCase : cases) {
            String expected = evaluationCase.expectedIntent();
            String actual = results.get(evaluationCase.caseId()).predictedIntent();
            labels.add(expected);
            labels.add(actual);
            if (Objects.equals(expected, actual)) {
                correct++;
                truePositive.merge(expected, 1L, Long::sum);
            } else {
                falsePositive.merge(actual, 1L, Long::sum);
                falseNegative.merge(expected, 1L, Long::sum);
            }
        }
        double macroF1 = labels.stream().mapToDouble(label -> {
            long tp = truePositive.getOrDefault(label, 0L);
            long fp = falsePositive.getOrDefault(label, 0L);
            long fn = falseNegative.getOrDefault(label, 0L);
            return harmonicMean(ratio(tp, tp + fp), ratio(tp, tp + fn));
        }).average().orElse(0.0);
        return new ClassificationMetrics(cases.size(), ratio(correct, cases.size()), macroF1);
    }

    private ExactMatchMetrics exactMatch(List<EvaluationCase> cases,
                                          Map<String, EvaluationResult> results,
                                          Function<EvaluationCase, String> expected,
                                          Function<EvaluationResult, String> actual) {
        long matches = cases.stream()
                .filter(evaluationCase -> Objects.equals(expected.apply(evaluationCase),
                        actual.apply(results.get(evaluationCase.caseId()))))
                .count();
        return new ExactMatchMetrics(cases.size(), matches, ratio(matches, cases.size()));
    }

    private RetrievalMetrics retrieval(List<EvaluationCase> cases,
                                        Map<String, EvaluationResult> results,
                                        int k) {
        long evaluated = 0;
        double recallSum = 0.0;
        double reciprocalRankSum = 0.0;
        for (EvaluationCase evaluationCase : cases) {
            Set<String> relevant = new HashSet<>(evaluationCase.relevantDocumentIds());
            if (relevant.isEmpty()) {
                continue;
            }
            evaluated++;
            List<String> retrieved = results.get(evaluationCase.caseId()).retrievedDocumentIds();
            List<String> topK = retrieved.subList(0, Math.min(k, retrieved.size()));
            long matches = topK.stream().distinct().filter(relevant::contains).count();
            recallSum += ratio(matches, relevant.size());
            for (int index = 0; index < topK.size(); index++) {
                if (relevant.contains(topK.get(index))) {
                    reciprocalRankSum += 1.0 / (index + 1);
                    break;
                }
            }
        }
        return new RetrievalMetrics(k, evaluated,
                evaluated == 0 ? 0.0 : recallSum / evaluated,
                evaluated == 0 ? 0.0 : reciprocalRankSum / evaluated);
    }

    private EndToEndMetrics endToEnd(List<EvaluationCase> cases, Map<String, EvaluationResult> results) {
        long successful = 0;
        long expectedMatches = 0;
        for (EvaluationCase evaluationCase : cases) {
            boolean actual = results.get(evaluationCase.caseId()).taskSuccess();
            successful += actual ? 1 : 0;
            expectedMatches += actual == evaluationCase.expectedTaskSuccess() ? 1 : 0;
        }
        return new EndToEndMetrics(cases.size(), successful, ratio(successful, cases.size()),
                ratio(expectedMatches, cases.size()));
    }

    private BinaryClassificationMetrics handoff(List<EvaluationCase> cases,
                                                  Map<String, EvaluationResult> results) {
        long tp = 0;
        long fp = 0;
        long fn = 0;
        long tn = 0;
        for (EvaluationCase evaluationCase : cases) {
            boolean expected = evaluationCase.expectedHandoff();
            boolean actual = results.get(evaluationCase.caseId()).handoff();
            if (expected && actual) {
                tp++;
            } else if (!expected && actual) {
                fp++;
            } else if (expected) {
                fn++;
            } else {
                tn++;
            }
        }
        double precision = ratio(tp, tp + fp);
        double recall = ratio(tp, tp + fn);
        return new BinaryClassificationMetrics(cases.size(), tp, fp, fn, tn, precision, recall,
                harmonicMean(precision, recall));
    }

    private RuntimeMetrics runtime(List<EvaluationResult> results) {
        List<Long> latencies = results.stream().map(EvaluationResult::latencyMillis)
                .filter(Objects::nonNull).sorted().toList();
        long promptTokens = results.stream().map(EvaluationResult::promptTokens).filter(Objects::nonNull)
                .mapToLong(Integer::longValue).sum();
        long completionTokens = results.stream().map(EvaluationResult::completionTokens).filter(Objects::nonNull)
                .mapToLong(Integer::longValue).sum();
        return new RuntimeMetrics(
                latencies.size(),
                latencies.stream().mapToLong(Long::longValue).average().orElse(0.0),
                percentile(latencies, 0.50),
                percentile(latencies, 0.95),
                promptTokens,
                completionTokens,
                promptTokens + completionTokens
        );
    }

    private Map<String, Long> failureCounts(List<EvaluationResult> results) {
        Map<String, Long> counts = new LinkedHashMap<>();
        results.stream()
                .filter(result -> result.errorComponent() != null || result.errorCode() != null)
                .sorted(Comparator.comparing(EvaluationResult::caseId))
                .forEach(result -> {
                    String component = result.errorComponent() == null ? "UNKNOWN" : result.errorComponent();
                    String code = result.errorCode() == null ? "UNKNOWN" : result.errorCode();
                    counts.merge(component + ":" + code, 1L, Long::sum);
                });
        return counts;
    }

    private String requireSingleDatasetVersion(List<EvaluationCase> cases, List<EvaluationResult> results) {
        Set<String> versions = new HashSet<>();
        cases.forEach(value -> versions.add(value.datasetVersion()));
        results.forEach(value -> versions.add(value.datasetVersion()));
        if (versions.size() != 1 || versions.contains(null)) {
            throw new IllegalArgumentException("cases and results must use one matching datasetVersion");
        }
        return versions.iterator().next();
    }

    private Map<String, EvaluationResult> indexResults(List<EvaluationResult> results) {
        Map<String, EvaluationResult> indexed = new HashMap<>();
        for (EvaluationResult result : results) {
            if (indexed.put(result.caseId(), result) != null) {
                throw new IllegalArgumentException("duplicate result caseId: " + result.caseId());
            }
        }
        return indexed;
    }

    private void ensureExactCaseCoverage(List<EvaluationCase> cases, Map<String, EvaluationResult> results) {
        Set<String> caseIds = new HashSet<>();
        for (EvaluationCase evaluationCase : cases) {
            if (!caseIds.add(evaluationCase.caseId())) {
                throw new IllegalArgumentException("duplicate case caseId: " + evaluationCase.caseId());
            }
            if (!results.containsKey(evaluationCase.caseId())) {
                throw new IllegalArgumentException("missing result for caseId: " + evaluationCase.caseId());
            }
        }
        List<String> unexpected = new ArrayList<>(results.keySet());
        unexpected.removeAll(caseIds);
        if (!unexpected.isEmpty()) {
            throw new IllegalArgumentException("unexpected result caseId: " + unexpected.get(0));
        }
    }

    private double percentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        if (sortedValues.size() == 1) {
            return sortedValues.get(0);
        }
        double index = percentile * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        double fraction = index - lower;
        return sortedValues.get(lower) + fraction * (sortedValues.get(upper) - sortedValues.get(lower));
    }

    private double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private double harmonicMean(double left, double right) {
        return left + right == 0.0 ? 0.0 : 2.0 * left * right / (left + right);
    }
}
