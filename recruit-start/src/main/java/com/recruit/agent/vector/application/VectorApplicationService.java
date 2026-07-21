package com.recruit.agent.vector.application;

import com.recruit.agent.vector.domain.EmbeddingService;
import com.recruit.agent.vector.domain.ProjectSimilarity;
import com.recruit.agent.vector.domain.VectorSearchRepository;

import java.util.Optional;

public class VectorApplicationService {

    private final EmbeddingService embeddingService;
    private final VectorSearchRepository repository;

    public VectorApplicationService(EmbeddingService embeddingService, VectorSearchRepository repository) {
        this.embeddingService = embeddingService;
        this.repository = repository;
    }

    public void indexJobDescription(Long jobId, String content) {
        repository.saveJobDescription(jobId, content, embeddingService.embed(content));
    }

    public void indexResumeProject(Long candidateId, Long projectId, String content) {
        repository.saveResumeProject(candidateId, projectId, content, embeddingService.embed(content));
    }

    public Optional<ProjectSimilarity> findBestProjectSimilarity(Long jobId, Long candidateId, String queryText) {
        return repository.findBestProjectSimilarity(jobId, candidateId, embeddingService.embed(queryText));
    }
}
