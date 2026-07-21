package com.recruit.agent.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.ai.domain.LlmCallResult;
import com.recruit.agent.ai.domain.RawLlmClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleRawLlmClient implements RawLlmClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiProperties properties;
    private final HttpClient httpClient;

    public OpenAiCompatibleRawLlmClient(AiProperties properties) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalArgumentException("recruit.ai.api-key 不能为空");
        }
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public LlmCallResult structuredChat(String systemPrompt, String userPrompt) {
        long started = System.currentTimeMillis();
        try {
            Map<String, Object> body = Map.of(
                    "model", properties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "response_format", Map.of("type", "json_object")
            );
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getBaseUrl()))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("LLM 调用失败: HTTP " + response.statusCode() + " " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            Integer promptTokens = readNullableInt(root.path("usage").path("prompt_tokens"));
            Integer completionTokens = readNullableInt(root.path("usage").path("completion_tokens"));
            return new LlmCallResult(
                    modelName(),
                    content,
                    promptTokens,
                    completionTokens,
                    System.currentTimeMillis() - started
            );
        } catch (Exception exception) {
            throw new IllegalStateException("LLM 调用失败", exception);
        }
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    private Integer readNullableInt(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asInt();
    }
}
