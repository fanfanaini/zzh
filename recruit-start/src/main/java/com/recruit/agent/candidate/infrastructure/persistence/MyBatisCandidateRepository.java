package com.recruit.agent.candidate.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.common.id.TimestampIdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisCandidateRepository implements CandidateRepository {

    private final CandidateMapper mapper;
    private final CandidatePoConverter converter = new CandidatePoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisCandidateRepository(CandidateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Candidate save(Candidate candidate) {
        if (mapper.selectById(candidate.id()) == null) {
            mapper.insert(converter.toPo(candidate));
        } else {
            mapper.updateById(converter.toPo(candidate));
        }
        return candidate;
    }

    @Override
    public Optional<Candidate> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(converter::toDomain);
    }

    @Override
    public List<Candidate> findAll() {
        return mapper.selectList(Wrappers.<CandidatePo>lambdaQuery()
                        .orderByDesc(CandidatePo::getCreatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public long nextCandidateId() {
        return idGenerator.nextId();
    }
}
