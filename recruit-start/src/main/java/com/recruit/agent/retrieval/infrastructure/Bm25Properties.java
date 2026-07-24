package com.recruit.agent.retrieval.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("recruit.retrieval.bm25")
public class Bm25Properties {
    private float k1 = 1.2f;
    private float b = 0.75f;

    public float getK1() {
        return k1;
    }

    public void setK1(float k1) {
        if (k1 < 0 || !Float.isFinite(k1)) {
            throw new IllegalArgumentException("BM25 k1 must be finite and non-negative");
        }
        this.k1 = k1;
    }

    public float getB() {
        return b;
    }

    public void setB(float b) {
        if (b < 0 || b > 1 || !Float.isFinite(b)) {
            throw new IllegalArgumentException("BM25 b must be between 0 and 1");
        }
        this.b = b;
    }
}
