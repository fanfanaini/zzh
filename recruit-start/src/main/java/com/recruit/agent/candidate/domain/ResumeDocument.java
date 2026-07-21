package com.recruit.agent.candidate.domain;

import java.time.Instant;

public record ResumeDocument(
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
        Instant uploadedAt,
        Instant updatedAt,
        String structuredDetailJson
) {

    public ResumeDocument(Long id, Long candidateId, String originalFilename, String contentType, long fileSize,
                          String storageObjectName, ResumeParseStatus parseStatus, String rawText, Instant uploadedAt) {
        this(id, candidateId, originalFilename, contentType, fileSize, storageObjectName, parseStatus, rawText,
                null, null, uploadedAt, uploadedAt, null);
    }

    public ResumeDocument {
        if (id == null) {
            throw new IllegalArgumentException("简历 ID 不能为空");
        }
        if (candidateId == null) {
            throw new IllegalArgumentException("候选人 ID 不能为空");
        }
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("简历文件名不能为空");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("简历文件大小必须大于 0");
        }
        if (storageObjectName == null || storageObjectName.isBlank()) {
            throw new IllegalArgumentException("文件存储对象名不能为空");
        }
        if (parseStatus == null) {
            parseStatus = ResumeParseStatus.UPLOADED;
        }
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = uploadedAt;
        }
    }

    public ResumeDocument markParsing() {
        return new ResumeDocument(id, candidateId, originalFilename, contentType, fileSize,
                storageObjectName, ResumeParseStatus.PARSING, rawText, null, parsedAt,
                uploadedAt, Instant.now(), structuredDetailJson);
    }

    public ResumeDocument markParsed(String parsedRawText) {
        Instant now = Instant.now();
        return new ResumeDocument(id, candidateId, originalFilename, contentType, fileSize,
                storageObjectName, ResumeParseStatus.PARSED, parsedRawText, null, now,
                uploadedAt, now, structuredDetailJson);
    }

    public ResumeDocument markFailed(String errorMessage) {
        return new ResumeDocument(id, candidateId, originalFilename, contentType, fileSize,
                storageObjectName, ResumeParseStatus.FAILED, rawText, errorMessage, parsedAt,
                uploadedAt, Instant.now(), structuredDetailJson);
    }

    public ResumeDocument withStructuredDetailJson(String json) {
        return new ResumeDocument(id, candidateId, originalFilename, contentType, fileSize, storageObjectName,
                parseStatus, rawText, parseErrorMessage, parsedAt, uploadedAt, Instant.now(), json);
    }
}
