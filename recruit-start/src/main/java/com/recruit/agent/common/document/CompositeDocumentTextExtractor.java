package com.recruit.agent.common.document;

import java.util.List;
import java.io.ByteArrayInputStream;

public class CompositeDocumentTextExtractor implements DocumentTextExtractor {

    private final List<DocumentTextExtractor> delegates;

    public CompositeDocumentTextExtractor(List<DocumentTextExtractor> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public boolean supports(DocumentContent document) {
        return delegates.stream().anyMatch(delegate -> delegate.supports(document));
    }

    @Override
    public ExtractedText extract(DocumentContent document) {
        try {
            byte[] content = document.inputStream().readAllBytes();
            RuntimeException lastFailure = null;
            boolean supported = false;
            for (DocumentTextExtractor delegate : delegates) {
                DocumentContent copy = new DocumentContent(document.originalFilename(), document.contentType(),
                        new ByteArrayInputStream(content));
                if (!delegate.supports(copy)) continue;
                supported = true;
                try {
                    ExtractedText result = delegate.extract(copy);
                    if (result.rawText() != null && !result.rawText().isBlank()) return result;
                } catch (RuntimeException exception) {
                    lastFailure = exception;
                }
            }
            if (!supported) throw new TextExtractionException("暂不支持该简历文件类型");
            if (lastFailure != null) throw lastFailure;
            throw new TextExtractionException("文档中没有提取到文字，请检查 OCR 配置");
        } catch (java.io.IOException exception) {
            throw new TextExtractionException("文档读取失败", exception);
        }
    }
}
