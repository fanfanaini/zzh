package com.recruit.agent.tool;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("recruit.tool.idempotency")
public class ToolIdempotencyProperties {
    private Duration resultTtl = Duration.ofHours(24);
    private Duration claimTtl = Duration.ofSeconds(30);
    private Duration pollInterval = Duration.ofMillis(25);
    private String keyPrefix = "recruit:tool:idempotency";

    public Duration getResultTtl() {
        return resultTtl;
    }

    public void setResultTtl(Duration resultTtl) {
        this.resultTtl = positive(resultTtl, "resultTtl");
    }

    public Duration getClaimTtl() {
        return claimTtl;
    }

    public void setClaimTtl(Duration claimTtl) {
        this.claimTtl = positive(claimTtl, "claimTtl");
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = positive(pollInterval, "pollInterval");
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            throw new IllegalArgumentException("keyPrefix must not be blank");
        }
        this.keyPrefix = keyPrefix.trim();
    }

    private Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
