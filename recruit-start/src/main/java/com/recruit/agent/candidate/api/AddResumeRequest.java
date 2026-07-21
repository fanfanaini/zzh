package com.recruit.agent.candidate.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AddResumeRequest(
        @NotBlank(message = "简历文件名不能为空")
        String originalFilename,
        String contentType,
        @Positive(message = "简历文件大小必须大于 0")
        long fileSize,
        @NotBlank(message = "文件存储对象名不能为空")
        String storageObjectName
) {
}
