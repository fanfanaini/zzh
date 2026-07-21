package com.recruit.agent.candidate.api;

import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeParseStatus;

import java.time.Instant;

public record ResumeResponse(
        Long id,
        Long candidateId,
        String originalFilename,
        String contentType,
        long fileSize,
        String storageObjectName,
        ResumeParseStatus parseStatus,
        String rawText,
        String parseErrorMessage,
        Instant parsedAt,
        Instant updatedAt,
        Instant uploadedAt
) {

    public static ResumeResponse from(ResumeDocument resume) {
        return new ResumeResponse(
                resume.id(),
                resume.candidateId(),
                resume.originalFilename(),
                resume.contentType(),
                resume.fileSize(),
                resume.storageObjectName(),
                resume.parseStatus(),
                resume.rawText(),
                resume.parseErrorMessage(),
                resume.parsedAt(),
                resume.updatedAt(),
                resume.uploadedAt()
        );
    }
}
