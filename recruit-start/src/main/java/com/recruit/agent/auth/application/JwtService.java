package com.recruit.agent.auth.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.auth.domain.UserAccount;
import com.recruit.agent.auth.domain.UserRole;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String secret;
    private final long expiresInSeconds;
    private final long refreshExpiresInSeconds;
    private final Clock clock;

    public JwtService(String secret, long expiresInSeconds, Clock clock) {
        this(secret, expiresInSeconds, 604800, clock);
    }

    public JwtService(String secret, long expiresInSeconds, long refreshExpiresInSeconds, Clock clock) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT 密钥长度至少需要 32 个字符");
        }
        this.secret = secret;
        this.expiresInSeconds = expiresInSeconds;
        this.refreshExpiresInSeconds = refreshExpiresInSeconds;
        this.clock = clock;
    }

    public String createToken(UserAccount user) {
        return createToken(user, "access", expiresInSeconds);
    }

    public String createRefreshToken(UserAccount user) {
        return createToken(user, "refresh", refreshExpiresInSeconds);
    }

    private String createToken(UserAccount user, String tokenType, long lifetimeSeconds) {
        Instant now = Instant.now(clock);
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.username());
        payload.put("uid", user.id());
        payload.put("roles", user.roles().stream().map(Enum::name).sorted().toList());
        payload.put("typ", tokenType);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(lifetimeSeconds).getEpochSecond());

        String signingInput = base64Url(json(header)) + "." + base64Url(json(payload));
        return signingInput + "." + sign(signingInput);
    }

    public AuthenticatedUser parse(String token) {
        return parse(token, "access");
    }

    public AuthenticatedUser parseRefreshToken(String token) {
        return parse(token, "refresh");
    }

    private AuthenticatedUser parse(String token, String expectedType) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT 格式不正确");
        }
        String signingInput = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(signingInput), parts[2])) {
            throw new IllegalArgumentException("JWT 签名无效");
        }
        Map<String, Object> payload = readPayload(parts[1]);
        if (!expectedType.equals(payload.get("typ"))) {
            throw new IllegalArgumentException("JWT 类型不正确");
        }
        long exp = ((Number) payload.get("exp")).longValue();
        if (Instant.now(clock).getEpochSecond() >= exp) {
            throw new IllegalArgumentException("JWT 已过期");
        }
        Long userId = ((Number) payload.get("uid")).longValue();
        String username = (String) payload.get("sub");
        @SuppressWarnings("unchecked")
        List<String> roleNames = (List<String>) payload.get("roles");
        Set<UserRole> roles = roleNames.stream()
                .map(UserRole::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        return new AuthenticatedUser(userId, username, roles);
    }

    public long expiresInSeconds() {
        return expiresInSeconds;
    }

    public long refreshExpiresInSeconds() {
        return refreshExpiresInSeconds;
    }

    private byte[] json(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JWT JSON 序列化失败", exception);
        }
    }

    private Map<String, Object> readPayload(String encodedPayload) {
        try {
            return objectMapper.readValue(Base64.getUrlDecoder().decode(encodedPayload), MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalArgumentException("JWT 载荷解析失败", exception);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 签名失败", exception);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigestSupport.equals(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MessageDigestSupport {
        private static boolean equals(byte[] left, byte[] right) {
            if (left.length != right.length) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < left.length; i++) {
                result |= left[i] ^ right[i];
            }
            return result == 0;
        }
    }
}
