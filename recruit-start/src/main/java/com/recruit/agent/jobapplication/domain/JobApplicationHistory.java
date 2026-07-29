package com.recruit.agent.jobapplication.domain;

import java.time.Instant;

public record JobApplicationHistory(
        Long id,
        Long applicationId,
        JobApplicationStage fromStage,
        JobApplicationStage toStage,
        Long operatorUserId,
        String reason,
        Instant createdAt
) implements java.io.Serializable {

    public JobApplicationHistory {
        if (id == null) {
            throw new IllegalArgumentException("招聘流转历史 ID 不能为空");
        }
        if (applicationId == null) {
            throw new IllegalArgumentException("投递记录 ID 不能为空");
        }
        if (toStage == null) {
            throw new IllegalArgumentException("目标阶段不能为空");
        }
        if (reason != null && reason.isBlank()) {
            reason = null;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
