package com.recruit.agent.evaluation.application;

import com.recruit.agent.evaluation.domain.RankingMetrics;
import com.recruit.agent.evaluation.domain.RetrievalComparisonReport;
import com.recruit.agent.evaluation.domain.RetrievalRankingSample;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RetrievalComparisonService {

    public RetrievalComparisonReport compare(List<RetrievalRankingSample> samples, int k) {
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("Retrieval samples must not be empty");
        }
        if (k < 1) {
            throw new IllegalArgumentException("k must be positive");
        }
        return new RetrievalComparisonReport(
                samples.size(),
                k,
                average(samples, sample -> recall(sample.candidateChunkIds(), sample.relevantChunkIds(),
                        sample.relevanceGrades(), Integer.MAX_VALUE)),
                metrics(samples, k, false),
                metrics(samples, k, true),
                average(samples, sample -> sample.rerankLatencyMillis()),
                average(samples, sample -> sample.degraded() ? 1 : 0)
        );
    }

    private RankingMetrics metrics(List<RetrievalRankingSample> samples, int k, boolean reranked) {
        return new RankingMetrics(
                average(samples, sample -> recall(ranking(sample, reranked), sample.relevantChunkIds(),
                        sample.relevanceGrades(), k)),
                average(samples, sample -> reciprocalRank(ranking(sample, reranked),
                        relevant(sample), k)),
                average(samples, sample -> ndcg(ranking(sample, reranked), grades(sample), k)),
                average(samples, sample -> hitRate(ranking(sample, reranked), relevant(sample), k))
        );
    }

    private List<Long> ranking(RetrievalRankingSample sample, boolean reranked) {
        return reranked ? sample.rerankedChunkIds() : sample.baselineChunkIds();
    }

    private Set<Long> relevant(RetrievalRankingSample sample) {
        if (!sample.relevantChunkIds().isEmpty()) {
            return sample.relevantChunkIds();
        }
        return sample.relevanceGrades().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Map<Long, Integer> grades(RetrievalRankingSample sample) {
        if (!sample.relevanceGrades().isEmpty()) {
            return sample.relevanceGrades();
        }
        return sample.relevantChunkIds().stream()
                .collect(java.util.stream.Collectors.toMap(id -> id, ignored -> 1));
    }

    private double recall(List<Long> ranking,
                          Set<Long> explicitRelevant,
                          Map<Long, Integer> relevanceGrades,
                          int k) {
        Set<Long> relevant = explicitRelevant.isEmpty()
                ? relevanceGrades.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet())
                : explicitRelevant;
        if (relevant.isEmpty()) {
            return 1;
        }
        long found = ranking.stream().limit(k).distinct().filter(relevant::contains).count();
        return (double) found / relevant.size();
    }

    private double reciprocalRank(List<Long> ranking, Set<Long> relevant, int k) {
        for (int index = 0; index < Math.min(k, ranking.size()); index++) {
            if (relevant.contains(ranking.get(index))) {
                return 1.0 / (index + 1);
            }
        }
        return 0;
    }

    private double hitRate(List<Long> ranking, Set<Long> relevant, int k) {
        return ranking.stream().limit(k).anyMatch(relevant::contains) ? 1 : 0;
    }

    private double ndcg(List<Long> ranking, Map<Long, Integer> grades, int k) {
        double actual = 0;
        for (int index = 0; index < Math.min(k, ranking.size()); index++) {
            actual += gain(grades.getOrDefault(ranking.get(index), 0), index);
        }
        List<Integer> idealGrades = new ArrayList<>(grades.values());
        idealGrades.sort(Comparator.reverseOrder());
        double ideal = 0;
        for (int index = 0; index < Math.min(k, idealGrades.size()); index++) {
            ideal += gain(idealGrades.get(index), index);
        }
        return ideal == 0 ? 1 : actual / ideal;
    }

    private double gain(int grade, int zeroBasedRank) {
        return (Math.pow(2, Math.max(0, grade)) - 1)
                / (Math.log(zeroBasedRank + 2) / Math.log(2));
    }

    private double average(List<RetrievalRankingSample> samples,
                           java.util.function.ToDoubleFunction<RetrievalRankingSample> metric) {
        return samples.stream().mapToDouble(metric).average().orElse(0);
    }
}
