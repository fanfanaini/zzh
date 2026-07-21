package com.recruit.agent.common.document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PlainTextDocumentTextExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(DocumentContent document) {
        String contentType = document.contentType();
        String filename = document.originalFilename();
        String normalizedFilename = filename == null ? "" : filename.toLowerCase();
        return (contentType != null && contentType.startsWith("text/"))
                || normalizedFilename.endsWith(".txt")
                || normalizedFilename.endsWith(".md");
    }

    @Override
    public ExtractedText extract(DocumentContent document) {
        if (!supports(document)) {
            throw new TextExtractionException("暂不支持该简历文件类型");
        }
        try {
            String rawText = new String(document.inputStream().readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .trim();
            return new ExtractedText(rawText);
        } catch (IOException exception) {
            throw new TextExtractionException("简历文本提取失败", exception);
        }
    }
}
