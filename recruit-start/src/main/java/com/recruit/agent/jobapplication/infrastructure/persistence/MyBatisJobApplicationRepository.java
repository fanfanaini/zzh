package com.recruit.agent.jobapplication.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.jobapplication.domain.JobApplication;
import com.recruit.agent.jobapplication.domain.JobApplicationHistory;
import com.recruit.agent.jobapplication.domain.JobApplicationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisJobApplicationRepository implements JobApplicationRepository {

    private final JobApplicationMapper applicationMapper;
    private final JobApplicationHistoryMapper historyMapper;
    private final JobApplicationPoConverter converter = new JobApplicationPoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisJobApplicationRepository(JobApplicationMapper applicationMapper,
                                           JobApplicationHistoryMapper historyMapper) {
        this.applicationMapper = applicationMapper;
        this.historyMapper = historyMapper;
    }

    @Override
    public JobApplication save(JobApplication application) {
        JobApplicationPo po = converter.toPo(application);
        if (applicationMapper.selectById(application.id()) == null) {
            applicationMapper.insert(po);
        } else {
            applicationMapper.updateById(po);
        }
        return application;
    }

    @Override
    public Optional<JobApplication> findById(Long id) {
        return Optional.ofNullable(applicationMapper.selectById(id))
                .map(converter::toDomain);
    }

    @Override
    public Optional<JobApplication> findByJobIdAndCandidateId(Long jobId, Long candidateId) {
        return applicationMapper.selectList(Wrappers.<JobApplicationPo>lambdaQuery()
                        .eq(JobApplicationPo::getJobId, jobId)
                        .eq(JobApplicationPo::getCandidateId, candidateId)
                        .last("limit 1"))
                .stream()
                .findFirst()
                .map(converter::toDomain);
    }

    @Override
    public List<JobApplication> findAll() {
        return applicationMapper.selectList(Wrappers.<JobApplicationPo>lambdaQuery()
                        .orderByDesc(JobApplicationPo::getUpdatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public JobApplicationHistory saveHistory(JobApplicationHistory history) {
        historyMapper.insert(converter.toPo(history));
        return history;
    }

    @Override
    public List<JobApplicationHistory> findHistoryByApplicationId(Long applicationId) {
        return historyMapper.selectList(Wrappers.<JobApplicationHistoryPo>lambdaQuery()
                        .eq(JobApplicationHistoryPo::getApplicationId, applicationId)
                        .orderByAsc(JobApplicationHistoryPo::getCreatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public long nextApplicationId() {
        return idGenerator.nextId();
    }

    @Override
    public long nextHistoryId() {
        return idGenerator.nextId();
    }
}
