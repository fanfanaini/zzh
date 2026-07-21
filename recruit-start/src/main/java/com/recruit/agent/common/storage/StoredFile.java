package com.recruit.agent.common.storage;

public record StoredFile(
        String originalFilename,
        String contentType,
        long size,
        String objectName
) {
}
