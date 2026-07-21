package com.recruit.agent.candidate.infrastructure.persistence;

import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeParseStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ResumePoConverter {

    public ResumePo toPo(ResumeDocument resume) {
        ResumePo po = new ResumePo();
        po.setId(resume.id());
        po.setCandidateId(resume.candidateId());
        po.setOriginalFilename(resume.originalFilename());
        po.setContentType(resume.contentType());
        po.setFileSize(resume.fileSize());
        po.setStorageObjectName(resume.storageObjectName());
        po.setParseStatus(resume.parseStatus().name());
        po.setRawText(resume.rawText());
        po.setParseErrorMessage(resume.parseErrorMessage());
        po.setParsedAt(resume.parsedAt() == null ? null : LocalDateTime.ofInstant(resume.parsedAt(), ZoneOffset.UTC));
        po.setUploadedAt(LocalDateTime.ofInstant(resume.uploadedAt(), ZoneOffset.UTC));
        po.setUpdatedAt(LocalDateTime.ofInstant(resume.updatedAt(), ZoneOffset.UTC));
        po.setStructuredDetailJson(resume.structuredDetailJson());
        return po;
    }

    public ResumeDocument toDomain(ResumePo po) {
        return new ResumeDocument(
                po.getId(),
                po.getCandidateId(),
                po.getOriginalFilename(),
                po.getContentType(),
                po.getFileSize(),
                po.getStorageObjectName(),
                ResumeParseStatus.valueOf(po.getParseStatus()),
                po.getRawText(),
                po.getParseErrorMessage(),
                po.getParsedAt() == null ? null : po.getParsedAt().toInstant(ZoneOffset.UTC),
                po.getUploadedAt().toInstant(ZoneOffset.UTC),
                po.getUpdatedAt() == null ? po.getUploadedAt().toInstant(ZoneOffset.UTC) : po.getUpdatedAt().toInstant(ZoneOffset.UTC),
                po.getStructuredDetailJson()
        );
    }
}
