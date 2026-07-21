package com.recruit.agent.vector.domain;

public record KnowledgeChunkMatch(Long documentId, Long chunkId, String sourceName, String content, double score) {}
