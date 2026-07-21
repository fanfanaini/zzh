package com.recruit.agent.knowledge.application;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.retrieval.application.HybridRetriever;
import com.recruit.agent.retrieval.domain.RankedRetrievalResult;
import com.recruit.agent.retrieval.domain.RetrievalRequest;
import com.recruit.agent.vector.domain.EmbeddingService;
import com.recruit.agent.vector.domain.KnowledgeChunkMatch;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import com.recruit.agent.vector.domain.KnowledgeDocumentSummary;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeApplicationService {
    private final EmbeddingService embeddingService;
    private final VectorSearchRepository repository;
    private final int chunkSize;
    private final int overlap;
    private final double minScore;
    private final HybridRetriever hybridRetriever;
    private final TimestampIdGenerator ids = new TimestampIdGenerator();

    public KnowledgeApplicationService(EmbeddingService embeddingService, VectorSearchRepository repository) {
        this(embeddingService, repository, 800, 100, 0, null);
    }

    public KnowledgeApplicationService(
            EmbeddingService embeddingService,
            VectorSearchRepository repository,
            int chunkSize,
            int overlap,
            double minScore
    ) {
        this(embeddingService, repository, chunkSize, overlap, minScore, null);
    }

    public KnowledgeApplicationService(
            EmbeddingService embeddingService,
            VectorSearchRepository repository,
            int chunkSize,
            int overlap,
            double minScore,
            HybridRetriever hybridRetriever
    ) {
        if (chunkSize < 200) {
            throw new IllegalArgumentException("知识切片大小不能小于 200");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("知识切片重叠长度必须大于等于 0 且小于切片大小");
        }
        this.embeddingService = embeddingService;
        this.repository = repository;
        this.chunkSize = chunkSize;
        this.overlap = overlap;
        this.minScore = Math.max(0, Math.min(100, minScore));
        this.hybridRetriever = hybridRetriever;
    }

    public IndexedKnowledgeDocument index(String sourceName, String content) {
        if (sourceName == null || sourceName.isBlank() || content == null || content.isBlank()) {
            throw new IllegalArgumentException("知识来源和内容不能为空");
        }
        long documentId = ids.nextId();
        List<String> chunks = split(content.trim());
        for (String chunk : chunks) {
            long chunkId = ids.nextId();
            repository.saveKnowledgeChunk(documentId, chunkId, sourceName.trim(), chunk, embeddingService.embed(chunk));
        }
        return new IndexedKnowledgeDocument(documentId, sourceName.trim(), chunks.size());
    }

    public List<KnowledgeChunkMatch> search(String query, int limit) {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("检索内容不能为空");
        int safeLimit = Math.max(1, Math.min(limit, 20));
        float[] queryEmbedding = embeddingService.embed(query);
        if (hybridRetriever != null) {
            return toKnowledgeMatches(hybridRetriever.search(
                    query,
                    queryEmbedding,
                    RetrievalRequest.of(safeLimit, minScore)
            ));
        }
        return repository.searchKnowledge(queryEmbedding, Math.min(safeLimit * 3, 60)).stream()
                .filter(match -> match.score() >= minScore)
                .limit(safeLimit)
                .toList();
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

    public List<KnowledgeDocumentSummary> listDocuments() {
        return repository.listKnowledgeDocuments();
    }

    public void delete(Long documentId) {
        if (documentId == null || !repository.deleteKnowledgeDocument(documentId)) {
            throw new java.util.NoSuchElementException("知识文档不存在: " + documentId);
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
            if (end == content.length()) break;
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
            if (value == '\n' || value == '。' || value == '！' || value == '？'
                    || value == '.' || value == '!' || value == '?') {
                return index;
            }
        }
        return desiredEnd;
    }

    public record IndexedKnowledgeDocument(Long documentId, String sourceName, int chunkCount) {}
}
