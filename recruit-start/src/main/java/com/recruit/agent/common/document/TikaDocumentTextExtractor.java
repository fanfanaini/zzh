package com.recruit.agent.common.document;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.IOException;
import java.util.Locale;

public class TikaDocumentTextExtractor implements DocumentTextExtractor {

    private final Tika tika;

    public TikaDocumentTextExtractor() {
        this(new Tika());
    }

    public TikaDocumentTextExtractor(Tika tika) {
        this.tika = tika;
    }

    @Override
    public boolean supports(DocumentContent document) {
        String contentType = document.contentType();
        String filename = document.originalFilename() == null
                ? ""
                : document.originalFilename().toLowerCase(Locale.ROOT);
        return "application/pdf".equals(contentType)
                || "application/msword".equals(contentType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)
                || filename.endsWith(".pdf")
                || filename.endsWith(".doc")
                || filename.endsWith(".docx");
    }

    @Override
    public ExtractedText extract(DocumentContent document) {
        if (!supports(document)) {
            throw new TextExtractionException("暂不支持该简历文件类型");
        }
        try {
            String rawText = tika.parseToString(document.inputStream()).trim();
            return new ExtractedText(rawText);
        } catch (IOException | TikaException exception) {
            throw new TextExtractionException("简历文本提取失败", exception);
        }
    }
}
