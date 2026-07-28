package com.recruit.agent.retrieval.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("recruit.retrieval.reranker")
public class RerankerProperties {
    private Mode mode = Mode.HEURISTIC;
    private double rrfWeight = 0.35;
    private double denseWeight = 0.20;
    private double sparseWeight = 0.20;
    private double contentCoverageWeight = 0.20;
    private double sourceCoverageWeight = 0.05;
    private URI endpoint;
    private String apiKey;
    private String model = "jina-reranker-v2-base-multilingual";
    private Duration timeout = Duration.ofSeconds(5);
    private int maxDocumentChars = 4_000;

    public enum Mode {
        DISABLED,
        HEURISTIC,
        HTTP
    }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode == null ? Mode.HEURISTIC : mode; }
    public double getRrfWeight() { return rrfWeight; }
    public void setRrfWeight(double value) { this.rrfWeight = weight(value, "rrfWeight"); }
    public double getDenseWeight() { return denseWeight; }
    public void setDenseWeight(double value) { this.denseWeight = weight(value, "denseWeight"); }
    public double getSparseWeight() { return sparseWeight; }
    public void setSparseWeight(double value) { this.sparseWeight = weight(value, "sparseWeight"); }
    public double getContentCoverageWeight() { return contentCoverageWeight; }
    public void setContentCoverageWeight(double value) {
        this.contentCoverageWeight = weight(value, "contentCoverageWeight");
    }
    public double getSourceCoverageWeight() { return sourceCoverageWeight; }
    public void setSourceCoverageWeight(double value) {
        this.sourceCoverageWeight = weight(value, "sourceCoverageWeight");
    }

    public double totalWeight() {
        return rrfWeight + denseWeight + sparseWeight + contentCoverageWeight + sourceCoverageWeight;
    }

    public URI getEndpoint() { return endpoint; }
    public void setEndpoint(URI endpoint) { this.endpoint = endpoint; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
    }
    public int getMaxDocumentChars() { return maxDocumentChars; }
    public void setMaxDocumentChars(int maxDocumentChars) {
        if (maxDocumentChars < 1 || maxDocumentChars > 100_000) {
            throw new IllegalArgumentException("maxDocumentChars must be between 1 and 100000");
        }
        this.maxDocumentChars = maxDocumentChars;
    }

    private double weight(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
        return value;
    }
}
