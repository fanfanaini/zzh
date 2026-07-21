package com.recruit.agent.common.document;

public record ExtractedText(String rawText) {

    public ExtractedText {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("提取文本不能为空");
        }
    }
}
