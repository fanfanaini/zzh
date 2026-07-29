package com.recruit.agent.memory.infrastructure;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.memory.domain.CandidateMemoryFact;
import com.recruit.agent.memory.domain.CandidateMemoryRepository;
import com.recruit.agent.memory.domain.CandidateMemorySourceType;
import com.recruit.agent.memory.domain.CandidateMemoryType;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@Profile("mysql")
public class JdbcCandidateMemoryRepository implements CandidateMemoryRepository {
    private final JdbcTemplate jdbc;
    private final TimestampIdGenerator ids = new TimestampIdGenerator();

    public JdbcCandidateMemoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CandidateMemoryFact save(CandidateMemoryFact fact) {
        int updated = jdbc.update("""
                        UPDATE candidate_memory
                        SET memory_type = ?, fact_key = ?, fact_value = ?, confidence = ?, source_type = ?,
                            source_id = ?, evidence = ?, active = ?, updated_at = ?
                        WHERE id = ?
                        """,
                fact.type().name(), fact.factKey(), fact.factValue(), fact.confidence(),
                fact.sourceType().name(), fact.sourceId(), fact.evidence(), fact.active(),
                Timestamp.from(fact.updatedAt()), fact.id());
        if (updated == 0) {
            jdbc.update("""
                            INSERT INTO candidate_memory
                            (id, candidate_id, memory_type, fact_key, fact_value, confidence, source_type,
                             source_id, evidence, active, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    fact.id(), fact.candidateId(), fact.type().name(), fact.factKey(), fact.factValue(),
                    fact.confidence(), fact.sourceType().name(), fact.sourceId(), fact.evidence(), fact.active(),
                    Timestamp.from(fact.createdAt()), Timestamp.from(fact.updatedAt()));
        }
        return fact;
    }

    @Override
    public Optional<CandidateMemoryFact> findById(Long id) {
        return jdbc.query("SELECT * FROM candidate_memory WHERE id = ?", this::map, id).stream().findFirst();
    }

    @Override
    public Optional<CandidateMemoryFact> findActiveByCandidateIdAndKey(Long candidateId, String factKey) {
        return jdbc.query("""
                        SELECT * FROM candidate_memory
                        WHERE candidate_id = ? AND fact_key = ? AND active = TRUE
                        ORDER BY updated_at DESC
                        LIMIT 1
                        """, this::map, candidateId, normalize(factKey))
                .stream().findFirst();
    }

    @Override
    public List<CandidateMemoryFact> findActiveByCandidateId(Long candidateId) {
        return jdbc.query("""
                        SELECT * FROM candidate_memory
                        WHERE candidate_id = ? AND active = TRUE
                        ORDER BY updated_at DESC
                        """, this::map, candidateId);
    }

    @Override
    public List<CandidateMemoryFact> findAllByCandidateId(Long candidateId) {
        return jdbc.query("""
                        SELECT * FROM candidate_memory
                        WHERE candidate_id = ?
                        ORDER BY updated_at DESC
                        """, this::map, candidateId);
    }

    @Override
    public long nextId() {
        return ids.nextId();
    }

    private CandidateMemoryFact map(ResultSet rs, int rowNum) throws SQLException {
        return new CandidateMemoryFact(
                rs.getLong("id"),
                rs.getLong("candidate_id"),
                CandidateMemoryType.valueOf(rs.getString("memory_type")),
                rs.getString("fact_key"),
                rs.getString("fact_value"),
                rs.getDouble("confidence"),
                CandidateMemorySourceType.valueOf(rs.getString("source_type")),
                nullableLong(rs, "source_id"),
                rs.getString("evidence"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
