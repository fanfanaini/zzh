package com.recruit.agent.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.UncheckedIOException;
import java.util.List;

public class StringListJsonCodec {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String encode(List<String> values) {
        try {
            if (values == null || values.isEmpty()) {
                return "[]";
            }
            return objectMapper.writeValueAsString(values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .toList());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("字符串列表 JSON 序列化失败", exception);
        }
    }

    public List<String> decode(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE).stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .toList();
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException("字符串列表 JSON 反序列化失败", exception);
        }
    }
}
