package com.recruit.agent.conversation.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recruit.conversation.memory")
public class ConversationMemoryProperties {
    private int tokenBudget = 2000;
    private int recentMessageLimit = 12;
    private int retryTokenBudget = 1000;

    public int getTokenBudget() { return tokenBudget; }
    public void setTokenBudget(int tokenBudget) { this.tokenBudget = tokenBudget; }
    public int getRecentMessageLimit() { return recentMessageLimit; }
    public void setRecentMessageLimit(int recentMessageLimit) { this.recentMessageLimit = recentMessageLimit; }
    public int getRetryTokenBudget() { return retryTokenBudget; }
    public void setRetryTokenBudget(int retryTokenBudget) { this.retryTokenBudget = retryTokenBudget; }
}
