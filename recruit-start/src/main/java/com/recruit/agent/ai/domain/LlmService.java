package com.recruit.agent.ai.domain;

public interface LlmService {

    <T> T structuredChat(LlmStructuredRequest<T> request);
}
