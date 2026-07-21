package com.recruit.agent.common.document;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class DocumentExtractionConfiguration {

    @Bean
    public DocumentTextExtractor documentTextExtractor(
            @Value("${recruit.ocr.enabled:false}") boolean ocrEnabled,
            @Value("${recruit.ocr.command:tesseract}") String ocrCommand,
            @Value("${recruit.ocr.language:chi_sim+eng}") String ocrLanguage) {
        java.util.ArrayList<DocumentTextExtractor> extractors = new java.util.ArrayList<>(List.of(
                new PlainTextDocumentTextExtractor(), new TikaDocumentTextExtractor()));
        if (ocrEnabled) extractors.add(new TesseractOcrDocumentTextExtractor(ocrCommand, ocrLanguage));
        return new CompositeDocumentTextExtractor(extractors);
    }
}
