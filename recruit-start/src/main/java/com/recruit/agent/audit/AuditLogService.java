package com.recruit.agent.audit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AuditLogService {
    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong ids = new AtomicLong(System.currentTimeMillis());
    private final List<AuditLog> localLogs = new CopyOnWriteArrayList<>();

    public AuditLogService(ObjectProvider<JdbcTemplate> jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate.getIfAvailable();
    }

    public void record(Long userId, String username, String method, String path, int statusCode,
                       long durationMillis, String clientIp) {
        AuditLog log = new AuditLog(ids.incrementAndGet(), userId, username, method, path, statusCode,
                durationMillis, clientIp, Instant.now());
        if (jdbcTemplate == null) {
            localLogs.add(log);
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO audit_log(id, user_id, username, http_method, request_path, status_code,
                                      duration_millis, client_ip, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, log.id(), log.userId(), log.username(), log.method(), log.path(), log.statusCode(),
                log.durationMillis(), log.clientIp(), java.sql.Timestamp.from(log.createdAt()));
    }

    public List<AuditLog> latest(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        if (jdbcTemplate == null) {
            return localLogs.stream().sorted(java.util.Comparator.comparing(AuditLog::createdAt).reversed())
                    .limit(safeLimit).toList();
        }
        return jdbcTemplate.query("""
                SELECT id, user_id, username, http_method, request_path, status_code, duration_millis,
                       client_ip, created_at FROM audit_log ORDER BY created_at DESC LIMIT ?
                """, (rs, row) -> new AuditLog(rs.getLong("id"), rs.getObject("user_id", Long.class),
                rs.getString("username"), rs.getString("http_method"), rs.getString("request_path"),
                rs.getInt("status_code"), rs.getLong("duration_millis"), rs.getString("client_ip"),
                rs.getTimestamp("created_at").toInstant()), safeLimit);
    }
}
