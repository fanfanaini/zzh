package com.recruit.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class RedisToolIdempotencyStore implements ToolIdempotencyStore {
    private static final DefaultRedisScript<List> CLAIM_SCRIPT = new DefaultRedisScript<>("""
            local result = redis.call('GET', KEYS[1])
            if result then
              return {'COMPLETED', result}
            end
            local acquired = redis.call('SET', KEYS[2], ARGV[1], 'NX', 'PX', ARGV[2])
            if acquired then
              return {'ACQUIRED', ''}
            end
            return {'PENDING', ''}
            """, List.class);
    private static final DefaultRedisScript<List> COMPLETE_SCRIPT = new DefaultRedisScript<>("""
            local owner = redis.call('GET', KEYS[2])
            if owner == ARGV[1] then
              redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
              redis.call('DEL', KEYS[2])
              return {'COMPLETED', ARGV[2]}
            end
            local result = redis.call('GET', KEYS[1])
            if result then
              return {'EXISTING', result}
            end
            return {'LOST', ''}
            """, List.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ToolIdempotencyProperties properties;

    public RedisToolIdempotencyStore(StringRedisTemplate redis,
                                     ObjectMapper objectMapper,
                                     ToolIdempotencyProperties properties) {
        if (redis == null || objectMapper == null || properties == null) {
            throw new IllegalArgumentException("Redis idempotency dependencies must not be null");
        }
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<ToolResult> find(String key) {
        String json = redis.opsForValue().get(resultKey(key));
        return json == null ? Optional.empty() : Optional.of(deserialize(json));
    }

    @Override
    public ToolResult saveIfAbsent(String key, ToolResult result) {
        Claim claim = claim(key, properties.getClaimTtl());
        return claim.acquired() ? complete(claim, result) : claim.completedResult();
    }

    @Override
    public Claim claim(String key, Duration leaseDuration) {
        String ownerToken = UUID.randomUUID().toString();
        Duration effectiveLease = max(properties.getClaimTtl(), leaseDuration);
        while (true) {
            List<?> response = redis.execute(
                    CLAIM_SCRIPT,
                    List.of(resultKey(key), claimKey(key)),
                    ownerToken,
                    String.valueOf(effectiveLease.toMillis())
            );
            String status = responsePart(response, 0);
            if ("ACQUIRED".equals(status)) {
                return Claim.acquired(key, ownerToken);
            }
            if ("COMPLETED".equals(status)) {
                return Claim.completed(key, deserialize(responsePart(response, 1)));
            }
            pause();
        }
    }

    @Override
    public ToolResult complete(Claim claim, ToolResult result) {
        requireAcquired(claim);
        String json = serialize(result);
        List<?> response = redis.execute(
                COMPLETE_SCRIPT,
                List.of(resultKey(claim.key()), claimKey(claim.key())),
                claim.ownerToken(),
                json,
                String.valueOf(properties.getResultTtl().toMillis())
        );
        String status = responsePart(response, 0);
        if ("COMPLETED".equals(status)) {
            return result;
        }
        if ("EXISTING".equals(status)) {
            return deserialize(responsePart(response, 1));
        }
        throw new IllegalStateException("Idempotency claim expired before the result was completed");
    }

    @Override
    public void release(Claim claim) {
        requireAcquired(claim);
        redis.execute(RELEASE_SCRIPT, List.of(claimKey(claim.key())), claim.ownerToken());
    }

    private void requireAcquired(Claim claim) {
        if (claim == null || !claim.acquired()) {
            throw new IllegalArgumentException("An acquired idempotency claim is required");
        }
    }

    private String serialize(ToolResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize tool idempotency result", exception);
        }
    }

    private ToolResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ToolResult.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize tool idempotency result", exception);
        }
    }

    private String responsePart(List<?> response, int index) {
        if (response == null || response.size() <= index || response.get(index) == null) {
            throw new IllegalStateException("Redis returned an invalid idempotency script response");
        }
        Object value = response.get(index);
        return value instanceof byte[] bytes ? new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                : value.toString();
    }

    private void pause() {
        try {
            Thread.sleep(properties.getPollInterval().toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an idempotent tool invocation", exception);
        }
    }

    private Duration max(Duration first, Duration second) {
        return second != null && second.compareTo(first) > 0 ? second : first;
    }

    private String resultKey(String scope) {
        return properties.getKeyPrefix() + ":result:" + scope;
    }

    private String claimKey(String scope) {
        return properties.getKeyPrefix() + ":claim:" + scope;
    }
}
