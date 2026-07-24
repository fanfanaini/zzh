package com.recruit.agent.retrieval.infrastructure;

import com.recruit.agent.retrieval.domain.CandidateReranker;
import com.recruit.agent.retrieval.domain.RankedRetrievalResult;
import com.recruit.agent.retrieval.domain.RerankOutcome;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HeuristicCandidateReranker implements CandidateReranker, AutoCloseable {
    private final RerankerProperties properties;
    private final Analyzer analyzer = new SmartChineseAnalyzer();

    public HeuristicCandidateReranker(RerankerProperties properties) {
        if (properties.totalWeight() <= 0) {
            throw new IllegalArgumentException("At least one reranker weight must be positive");
        }
        this.properties = properties;
    }

    @Override
    public RerankOutcome rerank(String query, List<RankedRetrievalResult> candidates, int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return new RerankOutcome(List.of(), "heuristic-smartcn-v1", false);
        }
        try {
            Set<String> queryTerms = terms(query);
            double maxRrf = max(candidates, ScoreType.RRF);
            double maxDense = max(candidates, ScoreType.DENSE);
            double maxSparse = max(candidates, ScoreType.SPARSE);
            List<ScoredCandidate> scored = new ArrayList<>(candidates.size());
            for (int index = 0; index < candidates.size(); index++) {
                RankedRetrievalResult candidate = candidates.get(index);
                double score = score(candidate, queryTerms, maxRrf, maxDense, maxSparse);
                scored.add(new ScoredCandidate(candidate, score, index));
            }
            return new RerankOutcome(scored.stream()
                    .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed()
                            .thenComparingInt(ScoredCandidate::originalRank))
                    .limit(limit)
                    .map(ScoredCandidate::candidate)
                    .toList(), "heuristic-smartcn-v1", false);
        } catch (IOException exception) {
            throw new IllegalStateException("Heuristic reranking analysis failed", exception);
        }
    }

    private double score(RankedRetrievalResult candidate,
                         Set<String> queryTerms,
                         double maxRrf,
                         double maxDense,
                         double maxSparse) throws IOException {
        double total = properties.totalWeight();
        return (
                properties.getRrfWeight() * normalize(candidate.fusedScore(), maxRrf)
                        + properties.getDenseWeight() * normalize(candidate.denseScore(), maxDense)
                        + properties.getSparseWeight() * normalize(candidate.sparseScore(), maxSparse)
                        + properties.getContentCoverageWeight() * coverage(queryTerms, terms(candidate.content()))
                        + properties.getSourceCoverageWeight() * coverage(queryTerms, terms(candidate.sourceName()))
        ) / total;
    }

    private Set<String> terms(String text) throws IOException {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> values = new HashSet<>();
        try (TokenStream stream = analyzer.tokenStream("text", text)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                values.add(term.toString());
            }
            stream.end();
        }
        return Set.copyOf(values);
    }

    private double coverage(Set<String> queryTerms, Set<String> candidateTerms) {
        if (queryTerms.isEmpty()) {
            return 0;
        }
        long matches = queryTerms.stream().filter(candidateTerms::contains).count();
        return (double) matches / queryTerms.size();
    }

    private double max(List<RankedRetrievalResult> candidates, ScoreType type) {
        return candidates.stream().map(type::value).filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue).max().orElse(0);
    }

    private double normalize(Double value, double maximum) {
        return value == null || maximum <= 0 ? 0 : Math.max(0, value / maximum);
    }

    private enum ScoreType {
        RRF {
            @Override Double value(RankedRetrievalResult result) { return result.fusedScore(); }
        },
        DENSE {
            @Override Double value(RankedRetrievalResult result) { return result.denseScore(); }
        },
        SPARSE {
            @Override Double value(RankedRetrievalResult result) { return result.sparseScore(); }
        };

        abstract Double value(RankedRetrievalResult result);
    }

    private record ScoredCandidate(RankedRetrievalResult candidate, double score, int originalRank) {
    }

    @Override
    public void close() {
        analyzer.close();
    }
}
