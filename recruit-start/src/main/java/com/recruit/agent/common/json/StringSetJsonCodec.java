package com.recruit.agent.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

public class StringSetJsonCodec {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String encode(Set<String> values) {
        try {
            if (values == null || values.isEmpty()) {
                return "[]";
            }
            return objectMapper.writeValueAsString(values.stream().sorted().toList());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("字符串集合 JSON 序列化失败", exception);
        }
    }

    public Set<String> decode(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Set.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE).stream()
                    .filter(value -> value != null && !value.isBlank())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException("字符串集合 JSON 反序列化失败", exception);
        }
    }
}
