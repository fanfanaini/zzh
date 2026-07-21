package com.recruit.agent.common.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlainTextDocumentTextExtractorTest {

    private final PlainTextDocumentTextExtractor extractor = new PlainTextDocumentTextExtractor();

    @Test
    void supportsPlainTextDocuments() {
        assertThat(extractor.supports(new DocumentContent(
                "resume.txt",
                "text/plain",
                new ByteArrayInputStream(new byte[0])
        ))).isTrue();
    }

    @Test
    void extractsUtf8PlainText() {
        ExtractedText text = extractor.extract(new DocumentContent(
                "resume.txt",
                "text/plain",
                new ByteArrayInputStream("Java\r\nSpring Boot".getBytes(StandardCharsets.UTF_8))
        ));

        assertThat(text.rawText()).isEqualTo("Java\nSpring Boot");
    }

    @Test
    void rejectsUnsupportedDocumentType() {
        assertThatThrownBy(() -> extractor.extract(new DocumentContent(
                "resume.pdf",
                "application/pdf",
                new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.UTF_8))
        ))).isInstanceOf(TextExtractionException.class)
                .hasMessageContaining("暂不支持");
    }
}
