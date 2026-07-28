package com.recruit.agent.retrieval.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.retrieval.domain.CandidateReranker;
import com.recruit.agent.retrieval.domain.RankedRetrievalResult;
import com.recruit.agent.retrieval.domain.RerankOutcome;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Calls the documented Jina /v1/rerank contract without exposing the provider to domain code. */
public final class HttpJinaCandidateReranker implements CandidateReranker {
    private final RerankerProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient client;

    public HttpJinaCandidateReranker(RerankerProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(requireTimeout(properties))
                .build());
    }

    HttpJinaCandidateReranker(RerankerProperties properties, ObjectMapper objectMapper, HttpClient client) {
        this.properties = requireProperties(properties);
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.client = java.util.Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public RerankOutcome rerank(String query, List<RankedRetrievalResult> candidates, int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return new RerankOutcome(List.of(), "jina-cross-encoder", false);
        }
        try {
            HttpResponse<String> response = client.send(request(query, candidates, limit),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Cross-encoder returned HTTP " + response.statusCode());
            }
            return parse(response.body(), candidates, limit);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cross-encoder request interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Cross-encoder request failed", exception);
        }
    }

    private HttpRequest request(String query, List<RankedRetrievalResult> candidates, int limit)
            throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("query", query);
        payload.put("documents", candidates.stream()
                .map(candidate -> truncate(candidate.content(), properties.getMaxDocumentChars()))
                .toList());
        payload.put("top_n", Math.min(limit, candidates.size()));
        payload.put("return_documents", false);
        return HttpRequest.newBuilder(properties.getEndpoint())
                .timeout(properties.getTimeout())
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
    }

    private RerankOutcome parse(String body, List<RankedRetrievalResult> candidates, int limit)
            throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) {
            throw new IllegalStateException("Cross-encoder response has no results");
        }
        List<ScoredCandidate> scored = new ArrayList<>();
        boolean[] seen = new boolean[candidates.size()];
        for (JsonNode item : results) {
            int index = item.path("index").asInt(-1);
            double score = item.path("relevance_score").asDouble(Double.NaN);
            if (index < 0 || index >= candidates.size() || seen[index] || !Double.isFinite(score)) {
                throw new IllegalStateException("Cross-encoder returned invalid candidate scores");
            }
            seen[index] = true;
            scored.add(new ScoredCandidate(candidates.get(index), score, index));
        }
        String model = root.path("model").asText(properties.getModel());
        return new RerankOutcome(scored.stream()
                .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed()
                        .thenComparingInt(ScoredCandidate::index))
                .limit(limit)
                .map(ScoredCandidate::candidate)
                .toList(), "jina-cross-encoder:" + safeModel(model), false);
    }

    private static RerankerProperties requireProperties(RerankerProperties properties) {
        if (properties == null || properties.getEndpoint() == null
                || !isHttp(properties.getEndpoint())
                || properties.getApiKey() == null || properties.getApiKey().isBlank()
                || properties.getModel() == null || properties.getModel().isBlank()) {
            throw new IllegalArgumentException("HTTP reranker requires endpoint, apiKey, and model");
        }
        return properties;
    }

    private static Duration requireTimeout(RerankerProperties properties) {
        return requireProperties(properties).getTimeout();
    }

    private static boolean isHttp(URI endpoint) {
        return endpoint.getHost() != null && ("http".equalsIgnoreCase(endpoint.getScheme())
                || "https".equalsIgnoreCase(endpoint.getScheme()));
    }

    private static String safeModel(String model) {
        return model == null || model.isBlank() ? "unknown" : model.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record ScoredCandidate(RankedRetrievalResult candidate, double score, int index) {
    }
}
