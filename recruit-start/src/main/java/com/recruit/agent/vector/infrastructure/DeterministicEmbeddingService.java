package com.recruit.agent.vector.infrastructure;

import com.recruit.agent.vector.domain.EmbeddingService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class DeterministicEmbeddingService implements EmbeddingService {

    public static final int DIMENSIONS = 1536;

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vector;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String token : normalized.split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")) {
            if (token.isBlank()) {
                continue;
            }
            int bucket = bucket(token);
            vector[bucket] += 1.0f;
        }
        addCjkTokens(normalized, vector);
        normalize(vector);
        return vector;
    }

    private void addCjkTokens(String text, float[] vector) {
        StringBuilder cjk = new StringBuilder();
        text.codePoints()
                .filter(this::isCjk)
                .forEach(cjk::appendCodePoint);
        for (int i = 0; i < cjk.length(); i++) {
            vector[bucket(cjk.substring(i, i + 1))] += 0.5f;
            if (i + 2 <= cjk.length()) {
                vector[bucket(cjk.substring(i, i + 2))] += 1.0f;
            }
        }
    }

    private boolean isCjk(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }

    private int bucket(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            int value = ((hash[0] & 0xff) << 24)
                    | ((hash[1] & 0xff) << 16)
                    | ((hash[2] & 0xff) << 8)
                    | (hash[3] & 0xff);
            return Math.floorMod(value, DIMENSIONS);
        } catch (Exception exception) {
            throw new IllegalStateException("生成本地向量失败", exception);
        }
    }

    private void normalize(float[] vector) {
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0) {
            return;
        }
        float scale = (float) (1 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= scale;
        }
    }
}
