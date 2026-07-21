package com.recruit.agent.vector.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.vector.domain.EmbeddingService;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

public class OpenAiCompatibleEmbeddingService implements EmbeddingService {
    private final EmbeddingProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleEmbeddingService(EmbeddingProperties properties, RestClient.Builder builder,
                                            ObjectMapper objectMapper) {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            throw new IllegalArgumentException("recruit.embedding.base-url 不能为空");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalArgumentException("recruit.embedding.api-key 不能为空");
        }
        this.properties = properties;
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public float[] embed(String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("input", text);
        body.put("dimensions", properties.getDimensions());
        String response = restClient.post().uri(properties.getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body).retrieve().body(String.class);
        try {
            JsonNode values = objectMapper.readTree(response).path("data").path(0).path("embedding");
            if (!values.isArray() || values.size() != properties.getDimensions()) {
                throw new IllegalStateException("Embedding 返回维度不正确，期望 " + properties.getDimensions());
            }
            float[] embedding = new float[values.size()];
            for (int i = 0; i < values.size(); i++) embedding[i] = values.get(i).floatValue();
            return embedding;
        } catch (Exception exception) {
            throw new IllegalStateException("Embedding 响应解析失败", exception);
        }
    }
}
