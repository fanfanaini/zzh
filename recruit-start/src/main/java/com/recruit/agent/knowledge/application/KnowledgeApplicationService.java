package com.recruit.agent.knowledge.application;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.retrieval.application.HybridRetriever;
import com.recruit.agent.retrieval.domain.KnowledgeSparseIndex;
import com.recruit.agent.retrieval.domain.RankedRetrievalResult;
import com.recruit.agent.retrieval.domain.RetrievalFilters;
import com.recruit.agent.retrieval.domain.RetrievalRequest;
import com.recruit.agent.vector.domain.EmbeddingService;
import com.recruit.agent.vector.domain.KnowledgeChunk;
import com.recruit.agent.vector.domain.KnowledgeChunkMatch;
import com.recruit.agent.vector.domain.KnowledgeChunkWrite;
import com.recruit.agent.vector.domain.KnowledgeDocumentSummary;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KnowledgeApplicationService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeApplicationService.class);

    private final EmbeddingService embeddingService;
    private final VectorSearchRepository repository;
    private final int chunkSize;
    private final int overlap;
    private final double minScore;
    private final HybridRetriever hybridRetriever;
    private final KnowledgeSparseIndex sparseIndex;
    private final KnowledgeQueryRewriter queryRewriter;
    private final boolean multiQueryEnabled;
    private final int multiQueryMaxQueries;
    private final int multiQueryCandidateMultiplier;
    private final TimestampIdGenerator ids = new TimestampIdGenerator();

    public KnowledgeApplicationService(EmbeddingService embeddingService, VectorSearchRepository repository) {
        this(embeddingService, repository, 800, 100, 0, null, null);
    }

    public KnowledgeApplicationService(
            EmbeddingService embeddingService,
            VectorSearchRepository repository,
            int chunkSize,
            int overlap,
            double minScore
    ) {
        this(embeddingService, repository, chunkSize, overlap, minScore, null, null);
    }

    public KnowledgeApplicationService(
            EmbeddingService embeddingService,
            VectorSearchRepository repository,
            int chunkSize,
            int overlap,
            double minScore,
            HybridRetriever hybridRetriever
    ) {
        this(embeddingService, repository, chunkSize, overlap, minScore, hybridRetriever, null);
    }

    public KnowledgeApplicationService(
            EmbeddingService embeddingService,
            VectorSearchRepository repository,
            int chunkSize,
            int overlap,
            double minScore,
            HybridRetriever hybridRetriever,
            KnowledgeSparseIndex sparseIndex
    ) {
        this(embeddingService, repository, chunkSize, overlap, minScore, hybridRetriever, sparseIndex,
                (query, maxQueries) -> List.of(query), false, 1, 3);
    }

    public KnowledgeApplicationService(
            EmbeddingService embeddingService,
            VectorSearchRepository repository,
            int chunkSize,
            int overlap,
            double minScore,
            HybridRetriever hybridRetriever,
            KnowledgeSparseIndex sparseIndex,
            KnowledgeQueryRewriter queryRewriter,
            boolean multiQueryEnabled,
            int multiQueryMaxQueries,
            int multiQueryCandidateMultiplier
    ) {
        if (chunkSize < 200) {
            throw new IllegalArgumentException("Knowledge chunk size must be at least 200");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Knowledge chunk overlap must be non-negative and smaller than chunk size");
        }
        this.embeddingService = embeddingService;
        this.repository = repository;
        this.chunkSize = chunkSize;
        this.overlap = overlap;
        this.minScore = Math.max(0, Math.min(100, minScore));
        this.hybridRetriever = hybridRetriever;
        this.sparseIndex = sparseIndex;
        this.queryRewriter = queryRewriter == null ? (query, maxQueries) -> List.of(query) : queryRewriter;
        this.multiQueryEnabled = multiQueryEnabled;
        this.multiQueryMaxQueries = Math.max(1, Math.min(8, multiQueryMaxQueries));
        this.multiQueryCandidateMultiplier = Math.max(1, Math.min(10, multiQueryCandidateMultiplier));
    }

    public IndexedKnowledgeDocument index(String sourceName, String content) {
        if (sourceName == null || sourceName.isBlank() || content == null || content.isBlank()) {
            throw new IllegalArgumentException("Knowledge source and content must not be blank");
        }
        long documentId = ids.nextId();
        List<String> chunks = split(content.trim());
        List<KnowledgeChunkWrite> writes = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            long chunkId = ids.nextId();
            writes.add(new KnowledgeChunkWrite(
                    documentId, chunkId, sourceName.trim(), chunk, embeddingService.embed(chunk)));
        }
        repository.saveKnowledgeDocument(writes);
        synchronizeSparseIndex(writes);
        return new IndexedKnowledgeDocument(documentId, sourceName.trim(), chunks.size());
    }

    public List<KnowledgeChunkMatch> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<String> queries = rewrittenQueries(query);
        if (hybridRetriever != null) {
            return searchHybrid(queries, safeLimit);
        }
        return searchDenseOnly(queries, safeLimit);
    }

    public List<KnowledgeDocumentSummary> listDocuments() {
        return repository.listKnowledgeDocuments();
    }

    public void delete(Long documentId) {
        if (documentId == null || !repository.deleteKnowledgeDocument(documentId)) {
            throw new java.util.NoSuchElementException("Knowledge document does not exist: " + documentId);
        }
        if (sparseIndex != null) {
            try {
                sparseIndex.deleteDocument(documentId);
            } catch (RuntimeException exception) {
                log.error("BM25 document deletion failed for {}; startup rebuild will repair the index",
                        documentId, exception);
            }
        }
    }

    private List<String> rewrittenQueries(String query) {
        if (!multiQueryEnabled) {
            return List.of(query.trim());
        }
        List<String> rewritten = queryRewriter.rewrite(query, multiQueryMaxQueries);
        if (rewritten == null || rewritten.isEmpty()) {
            return List.of(query.trim());
        }
        return rewritten.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .limit(multiQueryMaxQueries)
                .toList();
    }

    private List<KnowledgeChunkMatch> searchHybrid(List<String> queries, int safeLimit) {
        RetrievalRequest request = new RetrievalRequest(
                safeLimit,
                Math.min(60, safeLimit * multiQueryCandidateMultiplier),
                minScore,
                RetrievalFilters.none());
        List<List<KnowledgeChunkMatch>> perQueryResults = new ArrayList<>();
        for (String rewrittenQuery : queries) {
            try {
                perQueryResults.add(toKnowledgeMatches(hybridRetriever.search(
                        rewrittenQuery,
                        embeddingService.embed(rewrittenQuery),
                        request
                )));
            } catch (RuntimeException embeddingOrDenseFailure) {
                log.warn("Dense retrieval unavailable for rewritten query; falling back to BM25-only retrieval: {}",
                        embeddingOrDenseFailure.getMessage());
                try {
                    perQueryResults.add(toKnowledgeMatches(hybridRetriever.searchSparseOnly(rewrittenQuery, request)));
                } catch (RuntimeException sparseFailure) {
                    log.warn("Sparse retrieval also failed for rewritten query: {}", sparseFailure.getMessage());
                }
            }
        }
        return mergeMatches(perQueryResults, safeLimit);
    }

    private List<KnowledgeChunkMatch> searchDenseOnly(List<String> queries, int safeLimit) {
        List<List<KnowledgeChunkMatch>> perQueryResults = new ArrayList<>();
        int candidateLimit = Math.min(60, safeLimit * multiQueryCandidateMultiplier);
        for (String rewrittenQuery : queries) {
            try {
                float[] queryEmbedding = embeddingService.embed(rewrittenQuery);
                perQueryResults.add(repository.searchKnowledge(queryEmbedding, candidateLimit).stream()
                        .filter(match -> match.score() >= minScore)
                        .toList());
            } catch (RuntimeException exception) {
                log.warn("Dense knowledge retrieval failed for rewritten query: {}", exception.getMessage());
            }
        }
        return mergeMatches(perQueryResults, safeLimit);
    }

    private List<KnowledgeChunkMatch> toKnowledgeMatches(List<RankedRetrievalResult> results) {
        double maxFusedScore = results.stream()
                .mapToDouble(RankedRetrievalResult::fusedScore)
                .max()
                .orElse(0);
        if (maxFusedScore <= 0) {
            return List.of();
        }
        return results.stream()
                .map(result -> new KnowledgeChunkMatch(
                        result.documentId(),
                        result.chunkId(),
                        result.sourceName(),
                        result.content(),
                        Math.round(result.fusedScore() / maxFusedScore * 10000.0) / 100.0
                ))
                .toList();
    }

    private List<KnowledgeChunkMatch> mergeMatches(List<List<KnowledgeChunkMatch>> perQueryResults, int limit) {
        Map<Long, AccumulatedMatch> merged = new LinkedHashMap<>();
        for (int queryIndex = 0; queryIndex < perQueryResults.size(); queryIndex++) {
            List<KnowledgeChunkMatch> results = perQueryResults.get(queryIndex);
            for (int rank = 0; rank < results.size(); rank++) {
                KnowledgeChunkMatch match = results.get(rank);
                double contribution = match.score() + 100.0 / (rank + 1) + (queryIndex == 0 ? 15.0 : 0.0);
                merged.compute(match.chunkId(), (ignored, existing) -> {
                    if (existing == null) {
                        return new AccumulatedMatch(match, contribution, 1);
                    }
                    return existing.add(match, contribution);
                });
            }
        }
        return merged.values().stream()
                .sorted(java.util.Comparator
                        .comparingDouble(AccumulatedMatch::rankingScore).reversed()
                        .thenComparing(value -> value.match().chunkId()))
                .limit(limit)
                .map(AccumulatedMatch::toMatch)
                .toList();
    }

    private void synchronizeSparseIndex(List<KnowledgeChunkWrite> writes) {
        if (sparseIndex == null) {
            return;
        }
        try {
            List<KnowledgeChunk> chunks = writes.stream().map(KnowledgeChunkWrite::chunk).toList();
            sparseIndex.upsertDocument(chunks);
        } catch (RuntimeException exception) {
            log.error("BM25 document update failed; startup rebuild will repair the index", exception);
        }
    }

    private List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int desiredEnd = Math.min(content.length(), start + chunkSize);
            int end = findBoundary(content, start, desiredEnd);
            String chunk = content.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end == content.length()) {
                break;
            }
            start = Math.max(start + 1, end - overlap);
        }
        return List.copyOf(chunks);
    }

    private int findBoundary(String content, int start, int desiredEnd) {
        if (desiredEnd == content.length()) {
            return desiredEnd;
        }
        int lowerBound = Math.max(start + chunkSize / 2, desiredEnd - 160);
        for (int index = desiredEnd; index >= lowerBound; index--) {
            char value = content.charAt(index - 1);
            if (value == '\n' || value == '。' || value == '；' || value == '，'
                    || value == '.' || value == '!' || value == '?') {
                return index;
            }
        }
        return desiredEnd;
    }

    public record IndexedKnowledgeDocument(Long documentId, String sourceName, int chunkCount) {}

    private record AccumulatedMatch(KnowledgeChunkMatch match, double rankingScore, int hits) {
        private AccumulatedMatch add(KnowledgeChunkMatch candidate, double contribution) {
            KnowledgeChunkMatch best = candidate.score() > match.score() ? candidate : match;
            return new AccumulatedMatch(best, rankingScore + contribution + 25.0, hits + 1);
        }

        private KnowledgeChunkMatch toMatch() {
            return new KnowledgeChunkMatch(match.documentId(), match.chunkId(), match.sourceName(),
                    match.content(), Math.round(Math.min(100.0, rankingScore / hits) * 100.0) / 100.0);
        }
    }
}
