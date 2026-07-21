package com.recruit.agent.candidate.application;

public record AddResumeCommand(
        Long candidateId,
        String originalFilename,
        String contentType,
        long fileSize,
        String storageObjectName
) {
}
