package com.recruit.agent.knowledge.application;

import java.util.List;

public interface KnowledgeQueryRewriter {
    List<String> rewrite(String query, int maxQueries);
}
