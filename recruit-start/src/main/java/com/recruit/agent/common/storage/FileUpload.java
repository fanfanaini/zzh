package com.recruit.agent.common.storage;

import java.io.InputStream;

public record FileUpload(
        String originalFilename,
        String contentType,
        long size,
        InputStream inputStream
) {

    public FileUpload {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("文件大小必须大于 0");
        }
        if (inputStream == null) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
    }
}
