package com.recruit.agent.vector.domain;

public interface EmbeddingService {

    float[] embed(String text);
}
