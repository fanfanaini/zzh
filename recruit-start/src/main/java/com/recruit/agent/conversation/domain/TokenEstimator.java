package com.recruit.agent.conversation.domain;

public interface TokenEstimator {
    int estimate(String text);

    String truncate(String text, int maximumTokens);
}
