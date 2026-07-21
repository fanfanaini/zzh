package com.recruit.agent.audit;

import java.time.Instant;

public record AuditLog(Long id, Long userId, String username, String method, String path,
                       int statusCode, long durationMillis, String clientIp, Instant createdAt) {
}
