package com.recruit.agent.common.storage;

import java.io.InputStream;

public record StoredFileContent(
        String objectName,
        String originalFilename,
        String contentType,
        long size,
        InputStream inputStream
) implements AutoCloseable {

    public StoredFileContent {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("文件对象名不能为空");
        }
        if (inputStream == null) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
    }

    @Override
    public void close() throws Exception {
        inputStream.close();
    }
}
