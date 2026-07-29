package com.recruit.agent.skill.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recruit.skills")
public class RecruitmentSkillProperties {
    private boolean enabled = true;
    private int matchLimit = 3;
    private int maxPromptChars = 6000;
    private double minScore = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMatchLimit() {
        return matchLimit;
    }

    public void setMatchLimit(int matchLimit) {
        this.matchLimit = matchLimit;
    }

    public int getMaxPromptChars() {
        return maxPromptChars;
    }

    public void setMaxPromptChars(int maxPromptChars) {
        this.maxPromptChars = maxPromptChars;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }
}
