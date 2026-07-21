package com.recruit.agent.candidate.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeRepository;
import com.recruit.agent.common.id.TimestampIdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisResumeRepository implements ResumeRepository {

    private final ResumeMapper mapper;
    private final ResumePoConverter converter = new ResumePoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisResumeRepository(ResumeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ResumeDocument save(ResumeDocument resume) {
        if (mapper.selectById(resume.id()) == null) {
            mapper.insert(converter.toPo(resume));
        } else {
            mapper.updateById(converter.toPo(resume));
        }
        return resume;
    }

    @Override
    public Optional<ResumeDocument> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(converter::toDomain);
    }

    @Override
    public List<ResumeDocument> findByCandidateId(Long candidateId) {
        return mapper.selectList(Wrappers.<ResumePo>lambdaQuery()
                        .eq(ResumePo::getCandidateId, candidateId)
                        .orderByDesc(ResumePo::getUploadedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public long nextResumeId() {
        return idGenerator.nextId();
    }
}
