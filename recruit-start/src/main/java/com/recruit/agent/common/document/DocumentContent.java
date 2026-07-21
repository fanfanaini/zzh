package com.recruit.agent.common.document;

import java.io.InputStream;

public record DocumentContent(
        String originalFilename,
        String contentType,
        InputStream inputStream
) {
}
