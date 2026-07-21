package com.recruit.agent.ai.domain;

public interface RawLlmClient {

    LlmCallResult structuredChat(String systemPrompt, String userPrompt);

    String modelName();
}
