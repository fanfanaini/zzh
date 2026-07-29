package com.recruit.agent.jobapplication.domain;

import java.time.Instant;

public record JobApplication(
        Long id,
        Long jobId,
        Long candidateId,
        JobApplicationStage stage,
        Long ownerUserId,
        String source,
        Long latestEvaluationId,
        Long latestConversationId,
        String rejectionReason,
        String note,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) implements java.io.Serializable {

    public JobApplication(Long id, Long jobId, Long candidateId, Long ownerUserId, String source, String note,
                          Instant createdAt) {
        this(id, jobId, candidateId, JobApplicationStage.APPLIED, ownerUserId, source, null, null, null, note,
                createdAt, createdAt, null);
    }

    public JobApplication {
        if (id == null) {
            throw new IllegalArgumentException("投递记录 ID 不能为空");
        }
        if (jobId == null) {
            throw new IllegalArgumentException("岗位 ID 不能为空");
        }
        if (candidateId == null) {
            throw new IllegalArgumentException("候选人 ID 不能为空");
        }
        if (stage == null) {
            stage = JobApplicationStage.APPLIED;
        }
        source = normalizeText(source);
        note = normalizeText(note);
        rejectionReason = normalizeText(rejectionReason);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (stage.terminal() && closedAt == null) {
            closedAt = updatedAt;
        }
        if (!stage.terminal()) {
            closedAt = null;
        }
    }

    public JobApplication moveTo(JobApplicationStage nextStage, Long operatorUserId, String reason,
                                 Long evaluationId, Long conversationId, String newNote) {
        if (nextStage == null) {
            throw new IllegalArgumentException("招聘阶段不能为空");
        }
        if (stage.terminal() && stage != nextStage) {
            throw new IllegalStateException("终态投递记录不能继续流转");
        }
        Instant now = Instant.now();
        String nextRejectionReason = nextStage == JobApplicationStage.REJECTED
                ? firstNonBlank(reason, rejectionReason)
                : null;
        return new JobApplication(
                id,
                jobId,
                candidateId,
                nextStage,
                operatorUserId == null ? ownerUserId : operatorUserId,
                source,
                evaluationId == null ? latestEvaluationId : evaluationId,
                conversationId == null ? latestConversationId : conversationId,
                nextRejectionReason,
                firstNonBlank(newNote, note),
                createdAt,
                now,
                nextStage.terminal() ? now : null
        );
    }

    public JobApplication attachEvaluation(Long evaluationId) {
        if (evaluationId == null) {
            return this;
        }
        return new JobApplication(id, jobId, candidateId, stage, ownerUserId, source, evaluationId,
                latestConversationId, rejectionReason, note, createdAt, Instant.now(), closedAt);
    }

    public JobApplication attachConversation(Long conversationId) {
        if (conversationId == null) {
            return this;
        }
        return new JobApplication(id, jobId, candidateId, stage, ownerUserId, source, latestEvaluationId,
                conversationId, rejectionReason, note, createdAt, Instant.now(), closedAt);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        String normalized = normalizeText(preferred);
        return normalized == null ? fallback : normalized;
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
