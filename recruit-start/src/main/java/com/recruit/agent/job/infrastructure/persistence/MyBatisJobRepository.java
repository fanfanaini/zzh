package com.recruit.agent.job.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.domain.JobRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisJobRepository implements JobRepository {

    private final JobMapper mapper;
    private final JobPoConverter converter = new JobPoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisJobRepository(JobMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Job save(Job job) {
        if (mapper.selectById(job.id()) == null) {
            mapper.insert(converter.toPo(job));
        } else {
            mapper.updateById(converter.toPo(job));
        }
        return job;
    }

    @Override
    public Optional<Job> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(converter::toDomain);
    }

    @Override
    public List<Job> findAll() {
        return mapper.selectList(Wrappers.<JobPo>lambdaQuery()
                        .orderByDesc(JobPo::getCreatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public long nextId() {
        return idGenerator.nextId();
    }
}
