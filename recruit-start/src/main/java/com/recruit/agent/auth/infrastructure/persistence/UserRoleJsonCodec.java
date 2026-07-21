package com.recruit.agent.auth.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.auth.domain.UserRole;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserRoleJsonCodec {

    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String encode(Set<UserRole> roles) {
        try {
            return objectMapper.writeValueAsString(roles.stream().map(Enum::name).sorted().toList());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("用户角色 JSON 序列化失败", exception);
        }
    }

    public Set<UserRole> decode(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Set.of(UserRole.HR);
            }
            return objectMapper.readValue(json, LIST_TYPE).stream()
                    .map(UserRole::valueOf)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException("用户角色 JSON 反序列化失败", exception);
        }
    }
}
