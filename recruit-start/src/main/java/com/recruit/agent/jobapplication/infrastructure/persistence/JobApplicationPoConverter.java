package com.recruit.agent.jobapplication.infrastructure.persistence;

import com.recruit.agent.jobapplication.domain.JobApplication;
import com.recruit.agent.jobapplication.domain.JobApplicationHistory;
import com.recruit.agent.jobapplication.domain.JobApplicationStage;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class JobApplicationPoConverter {

    public JobApplicationPo toPo(JobApplication application) {
        JobApplicationPo po = new JobApplicationPo();
        po.setId(application.id());
        po.setJobId(application.jobId());
        po.setCandidateId(application.candidateId());
        po.setStage(application.stage().name());
        po.setOwnerUserId(application.ownerUserId());
        po.setSource(application.source());
        po.setLatestEvaluationId(application.latestEvaluationId());
        po.setLatestConversationId(application.latestConversationId());
        po.setRejectionReason(application.rejectionReason());
        po.setNote(application.note());
        po.setCreatedAt(LocalDateTime.ofInstant(application.createdAt(), ZoneOffset.UTC));
        po.setUpdatedAt(LocalDateTime.ofInstant(application.updatedAt(), ZoneOffset.UTC));
        po.setClosedAt(application.closedAt() == null
                ? null
                : LocalDateTime.ofInstant(application.closedAt(), ZoneOffset.UTC));
        return po;
    }

    public JobApplication toDomain(JobApplicationPo po) {
        return new JobApplication(
                po.getId(),
                po.getJobId(),
                po.getCandidateId(),
                JobApplicationStage.valueOf(po.getStage()),
                po.getOwnerUserId(),
                po.getSource(),
                po.getLatestEvaluationId(),
                po.getLatestConversationId(),
                po.getRejectionReason(),
                po.getNote(),
                po.getCreatedAt().toInstant(ZoneOffset.UTC),
                po.getUpdatedAt() == null
                        ? po.getCreatedAt().toInstant(ZoneOffset.UTC)
                        : po.getUpdatedAt().toInstant(ZoneOffset.UTC),
                po.getClosedAt() == null ? null : po.getClosedAt().toInstant(ZoneOffset.UTC)
        );
    }

    public JobApplicationHistoryPo toPo(JobApplicationHistory history) {
        JobApplicationHistoryPo po = new JobApplicationHistoryPo();
        po.setId(history.id());
        po.setApplicationId(history.applicationId());
        po.setFromStage(history.fromStage() == null ? null : history.fromStage().name());
        po.setToStage(history.toStage().name());
        po.setOperatorUserId(history.operatorUserId());
        po.setReason(history.reason());
        po.setCreatedAt(LocalDateTime.ofInstant(history.createdAt(), ZoneOffset.UTC));
        return po;
    }

    public JobApplicationHistory toDomain(JobApplicationHistoryPo po) {
        return new JobApplicationHistory(
                po.getId(),
                po.getApplicationId(),
                po.getFromStage() == null ? null : JobApplicationStage.valueOf(po.getFromStage()),
                JobApplicationStage.valueOf(po.getToStage()),
                po.getOperatorUserId(),
                po.getReason(),
                po.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
