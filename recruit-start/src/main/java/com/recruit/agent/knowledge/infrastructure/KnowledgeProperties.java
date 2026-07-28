package com.recruit.agent.knowledge.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recruit.knowledge")
public class KnowledgeProperties {

    private int chunkSize = 800;
    private int overlap = 100;
    private double minScore = 35;
    private boolean multiQueryEnabled = true;
    private int multiQueryMaxQueries = 4;
    private int multiQueryCandidateMultiplier = 3;

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getOverlap() {
        return overlap;
    }

    public void setOverlap(int overlap) {
        this.overlap = overlap;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public boolean isMultiQueryEnabled() {
        return multiQueryEnabled;
    }

    public void setMultiQueryEnabled(boolean multiQueryEnabled) {
        this.multiQueryEnabled = multiQueryEnabled;
    }

    public int getMultiQueryMaxQueries() {
        return multiQueryMaxQueries;
    }

    public void setMultiQueryMaxQueries(int multiQueryMaxQueries) {
        this.multiQueryMaxQueries = Math.max(1, Math.min(8, multiQueryMaxQueries));
    }

    public int getMultiQueryCandidateMultiplier() {
        return multiQueryCandidateMultiplier;
    }

    public void setMultiQueryCandidateMultiplier(int multiQueryCandidateMultiplier) {
        this.multiQueryCandidateMultiplier = Math.max(1, Math.min(10, multiQueryCandidateMultiplier));
    }
}
