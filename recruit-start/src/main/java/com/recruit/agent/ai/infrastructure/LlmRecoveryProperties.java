package com.recruit.agent.ai.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("recruit.ai.recovery")
public class LlmRecoveryProperties {
    private int maxAttempts = 3;
    private Duration initialBackoff = Duration.ofMillis(200);
    private Duration maximumBackoff = Duration.ofSeconds(2);

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) {
        if (maxAttempts < 1 || maxAttempts > 5) throw new IllegalArgumentException("maxAttempts must be 1..5");
        this.maxAttempts = maxAttempts;
    }
    public Duration getInitialBackoff() { return initialBackoff; }
    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = nonNegative(initialBackoff, "initialBackoff");
    }
    public Duration getMaximumBackoff() { return maximumBackoff; }
    public void setMaximumBackoff(Duration maximumBackoff) {
        this.maximumBackoff = nonNegative(maximumBackoff, "maximumBackoff");
    }

    private Duration nonNegative(Duration value, String name) {
        if (value == null || value.isNegative()) throw new IllegalArgumentException(name + " must not be negative");
        return value;
    }
}
