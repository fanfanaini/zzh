package com.recruit.agent.screening.application;

import java.util.List;

public record BatchScreeningJob(Long taskId, Long jobId, List<Long> candidateIds) {
    public BatchScreeningJob {
        candidateIds = List.copyOf(candidateIds);
    }
}
