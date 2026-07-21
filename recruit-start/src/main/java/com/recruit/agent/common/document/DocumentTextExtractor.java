package com.recruit.agent.common.document;

public interface DocumentTextExtractor {

    boolean supports(DocumentContent document);

    ExtractedText extract(DocumentContent document);
}
