package com.recruit.agent.vector.api;

import com.recruit.agent.vector.domain.EmbeddingIndexRebuildJob;

import java.time.Instant;

public record EmbeddingRebuildJobResponse(
        String jobId,
        String targetVersion,
        String status,
        int totalChunks,
        int succeededChunks,
        int failedChunks,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage
) {
    public static EmbeddingRebuildJobResponse from(EmbeddingIndexRebuildJob job) {
        return new EmbeddingRebuildJobResponse(job.jobId(), job.targetVersion(), job.status(),
                job.totalChunks(), job.succeededChunks(), job.failedChunks(),
                job.startedAt(), job.finishedAt(), job.errorMessage());
    }
}
