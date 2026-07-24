package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.conversation.domain.TokenEstimator;

public class HeuristicTokenEstimator implements TokenEstimator {
    @Override
    public int estimate(String text) {
        if (text == null || text.isBlank()) return 0;
        int tokens = 0;
        int asciiRun = 0;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint <= 0x7f && !Character.isWhitespace(codePoint)) {
                asciiRun++;
            } else {
                tokens += (asciiRun + 3) / 4;
                asciiRun = 0;
                if (!Character.isWhitespace(codePoint)) tokens++;
            }
        }
        return tokens + (asciiRun + 3) / 4;
    }

    @Override
    public String truncate(String text, int maximumTokens) {
        if (text == null || text.isBlank() || maximumTokens <= 0) return "";
        String value = text.trim();
        if (estimate(value) <= maximumTokens) return value;
        int[] codePoints = value.codePoints().toArray();
        int low = 0;
        int high = codePoints.length;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            String candidate = new String(codePoints, 0, middle);
            if (estimate(candidate) <= maximumTokens) low = middle;
            else high = middle - 1;
        }
        return new String(codePoints, 0, low).trim();
    }
}
