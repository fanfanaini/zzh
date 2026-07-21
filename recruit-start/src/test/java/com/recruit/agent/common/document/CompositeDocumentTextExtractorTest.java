package com.recruit.agent.common.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeDocumentTextExtractorTest {

    @Test
    void routesToFirstSupportingExtractor() {
        CompositeDocumentTextExtractor extractor = new CompositeDocumentTextExtractor(List.of(
                new PlainTextDocumentTextExtractor()
        ));

        ExtractedText text = extractor.extract(new DocumentContent(
                "resume.txt",
                "text/plain",
                new ByteArrayInputStream("Java".getBytes(StandardCharsets.UTF_8))
        ));

        assertThat(text.rawText()).isEqualTo("Java");
    }

    @Test
    void rejectsUnsupportedDocumentType() {
        CompositeDocumentTextExtractor extractor = new CompositeDocumentTextExtractor(List.of(
                new PlainTextDocumentTextExtractor()
        ));

        assertThatThrownBy(() -> extractor.extract(new DocumentContent(
                "resume.pdf",
                "application/pdf",
                new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.UTF_8))
        ))).isInstanceOf(TextExtractionException.class)
                .hasMessageContaining("暂不支持");
    }
}
