package com.recruit.agent.candidate.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;

import java.time.LocalDateTime;

@TableName("resume")
public class ResumePo {

    @TableId
    private Long id;
    private Long candidateId;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String storageObjectName;
    private String parseStatus;
    private String rawText;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String parseErrorMessage;
    private LocalDateTime parsedAt;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private String structuredDetailJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStorageObjectName() {
        return storageObjectName;
    }

    public void setStorageObjectName(String storageObjectName) {
        this.storageObjectName = storageObjectName;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getParseErrorMessage() {
        return parseErrorMessage;
    }

    public void setParseErrorMessage(String parseErrorMessage) {
        this.parseErrorMessage = parseErrorMessage;
    }

    public LocalDateTime getParsedAt() {
        return parsedAt;
    }

    public void setParsedAt(LocalDateTime parsedAt) {
        this.parsedAt = parsedAt;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStructuredDetailJson() {
        return structuredDetailJson;
    }

    public void setStructuredDetailJson(String structuredDetailJson) {
        this.structuredDetailJson = structuredDetailJson;
    }
}
