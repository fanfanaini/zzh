package com.recruit.agent.vector.domain;

import java.time.Instant;

public record EmbeddingIndexRebuildJob(
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
}
