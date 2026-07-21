package com.recruit.agent.ai.infrastructure;

import com.recruit.agent.ai.domain.LlmCallResult;
import com.recruit.agent.ai.domain.RawLlmClient;

public class DisabledRawLlmClient implements RawLlmClient {

    @Override
    public LlmCallResult structuredChat(String systemPrompt, String userPrompt) {
        throw new IllegalStateException("AI capability is disabled");
    }

    @Override
    public String modelName() {
        return "disabled";
    }
}
