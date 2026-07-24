package com.recruit.agent.retrieval.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("recruit.retrieval.reranker")
public class RerankerProperties {
    private Mode mode = Mode.HEURISTIC;
    private double rrfWeight = 0.35;
    private double denseWeight = 0.20;
    private double sparseWeight = 0.20;
    private double contentCoverageWeight = 0.20;
    private double sourceCoverageWeight = 0.05;

    public enum Mode {
        DISABLED,
        HEURISTIC
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

    private double weight(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
        return value;
    }
}
