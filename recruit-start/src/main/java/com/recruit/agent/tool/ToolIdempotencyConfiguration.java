package com.recruit.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(ToolIdempotencyProperties.class)
public class ToolIdempotencyConfiguration {

    @Bean
    @Profile("!distributed")
    public ToolIdempotencyStore inMemoryToolIdempotencyStore() {
        return new InMemoryToolIdempotencyStore();
    }

    @Bean
    @Profile("distributed")
    public ToolIdempotencyStore redisToolIdempotencyStore(StringRedisTemplate redis,
                                                          ObjectMapper objectMapper,
                                                          ToolIdempotencyProperties properties) {
        return new RedisToolIdempotencyStore(redis, objectMapper, properties);
    }
}
