package com.recruit.agent.retrieval.application;

import com.recruit.agent.retrieval.domain.RankedRetrievalResult;
import com.recruit.agent.retrieval.domain.RetrievalHit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReciprocalRankFusion {

    private final int rankConstant;

    public ReciprocalRankFusion(int rankConstant) {
        if (rankConstant < 1) {
            throw new IllegalArgumentException("rankConstant must be positive");
        }
        this.rankConstant = rankConstant;
    }

    public List<RankedRetrievalResult> fuse(
            List<RetrievalHit> denseResults,
            List<RetrievalHit> sparseResults,
            int limit
    ) {
        if (limit <= 0) {
            return List.of();
        }
        Map<Long, Accumulator> accumulated = new HashMap<>();
        addChannel(denseResults, true, accumulated);
        addChannel(sparseResults, false, accumulated);

        return accumulated.values().stream()
                .map(Accumulator::toResult)
                .sorted(java.util.Comparator.comparingDouble(RankedRetrievalResult::fusedScore).reversed()
                        .thenComparingInt(this::bestRank)
                        .thenComparing(RankedRetrievalResult::chunkId))
                .limit(limit)
                .toList();
    }

    private void addChannel(List<RetrievalHit> results, boolean dense, Map<Long, Accumulator> accumulated) {
        if (results == null || results.isEmpty()) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        for (int index = 0; index < results.size(); index++) {
            RetrievalHit hit = results.get(index);
            if (hit == null || !seen.add(hit.chunkId())) {
                continue;
            }
            int rank = index + 1;
            Accumulator value = accumulated.computeIfAbsent(hit.chunkId(), ignored -> new Accumulator(hit));
            value.fusedScore += 1.0 / (rankConstant + rank);
            if (dense) {
                value.denseRank = rank;
                value.denseScore = hit.score();
            } else {
                value.sparseRank = rank;
                value.sparseScore = hit.score();
            }
        }
    }

    private int bestRank(RankedRetrievalResult result) {
        int denseRank = result.denseRank() == null ? Integer.MAX_VALUE : result.denseRank();
        int sparseRank = result.sparseRank() == null ? Integer.MAX_VALUE : result.sparseRank();
        return Math.min(denseRank, sparseRank);
    }

    private static final class Accumulator {
        private final RetrievalHit hit;
        private Integer denseRank;
        private Double denseScore;
        private Integer sparseRank;
        private Double sparseScore;
        private double fusedScore;

        private Accumulator(RetrievalHit hit) {
            this.hit = hit;
        }

        private RankedRetrievalResult toResult() {
            return new RankedRetrievalResult(hit.documentId(), hit.chunkId(), hit.sourceName(), hit.content(),
                    denseRank, denseScore, sparseRank, sparseScore, fusedScore);
        }
    }
}
