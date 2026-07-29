package com.recruit.agent.screening.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.domain.JobRepository;
import com.recruit.agent.jobapplication.application.JobApplicationPipelineService;
import com.recruit.agent.screening.domain.CandidateProfile;
import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.EvaluationRepository;
import com.recruit.agent.screening.domain.JobProfile;
import com.recruit.agent.screening.domain.ResumeScreeningService;
import com.recruit.agent.screening.domain.ScreeningRecommendation;
import com.recruit.agent.screening.domain.ScreeningResult;
import com.recruit.agent.screening.domain.ScreeningReviewRequest;
import com.recruit.agent.screening.domain.ScreeningReviewService;
import com.recruit.agent.vector.application.VectorApplicationService;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.List;

public class ScreeningApplicationService {

    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final ResumeScreeningService screeningService;
    private final EvaluationRepository evaluationRepository;
    private final Optional<VectorApplicationService> vectorApplicationService;
    private final Optional<ScreeningReviewService> screeningReviewService;
    private final Optional<JobApplicationPipelineService> jobApplicationPipelineService;

    public ScreeningApplicationService(
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ResumeScreeningService screeningService,
            EvaluationRepository evaluationRepository,
            Optional<VectorApplicationService> vectorApplicationService,
            Optional<ScreeningReviewService> screeningReviewService
    ) {
        this(jobRepository, candidateRepository, screeningService, evaluationRepository, vectorApplicationService,
                screeningReviewService, Optional.empty());
    }

    public ScreeningApplicationService(
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ResumeScreeningService screeningService,
            EvaluationRepository evaluationRepository,
            Optional<VectorApplicationService> vectorApplicationService,
            Optional<ScreeningReviewService> screeningReviewService,
            Optional<JobApplicationPipelineService> jobApplicationPipelineService
    ) {
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.screeningService = screeningService;
        this.evaluationRepository = evaluationRepository;
        this.vectorApplicationService = vectorApplicationService;
        this.screeningReviewService = screeningReviewService;
        this.jobApplicationPipelineService = jobApplicationPipelineService;
    }

    public ScreeningResult screen(ScreenCandidateCommand command) {
        Job job = jobRepository.findById(command.jobId())
                .orElseThrow(() -> new NoSuchElementException("岗位不存在: " + command.jobId()));
        Candidate candidate = candidateRepository.findById(command.candidateId())
                .orElseThrow(() -> new NoSuchElementException("候选人不存在: " + command.candidateId()));
        Double semanticScore = vectorApplicationService
                .flatMap(service -> service.findBestProjectSimilarity(job.id(), candidate.id(), job.description()))
                .map(similarity -> Math.max(0, Math.min(100, similarity.score())))
                .orElse(null);
        ScreeningResult ruleResult = screeningService.screen(toJobProfile(job), toCandidateProfile(candidate), semanticScore);
        ScreeningResult result = screeningReviewService
                .map(service -> service.review(new ScreeningReviewRequest(job, candidate, ruleResult, semanticScore)))
                .orElse(ruleResult);
        Evaluation evaluation = evaluationRepository.save(Evaluation.fromScreeningResult(
                evaluationRepository.nextEvaluationId(),
                job.id(),
                candidate.id(),
                result
        ));
        jobApplicationPipelineService.ifPresent(service ->
                service.markScreeningStarted(job.id(), candidate.id(), evaluation.id()));
        return result;
    }

    public List<Evaluation> rankings(Long jobId) {
        if (jobRepository.findById(jobId).isEmpty()) {
            throw new NoSuchElementException("岗位不存在: " + jobId);
        }
        return evaluationRepository.findLatestByJobId(jobId);
    }

    public List<Evaluation> rankings(Long jobId, Long candidateId, String keyword, ScreeningRecommendation recommendation) {
        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase();
        return rankings(jobId).stream()
                .filter(evaluation -> candidateId == null || evaluation.candidateId().equals(candidateId))
                .filter(evaluation -> recommendation == null || evaluation.recommendation() == recommendation)
                .filter(evaluation -> normalizedKeyword == null || normalizedKeyword.isBlank()
                        || candidateRepository.findById(evaluation.candidateId())
                        .map(candidate -> candidate.name().toLowerCase().contains(normalizedKeyword)
                                || candidate.skills().stream().anyMatch(skill -> skill.contains(normalizedKeyword)))
                        .orElse(false))
                .toList();
    }

    private JobProfile toJobProfile(Job job) {
        return new JobProfile(
                job.id(),
                job.name(),
                job.requiredSkills(),
                job.preferredSkills(),
                job.educationRequirement(),
                job.minExperienceYears(),
                java.util.Set.of()
        );
    }

    private CandidateProfile toCandidateProfile(Candidate candidate) {
        return new CandidateProfile(
                candidate.id(),
                candidate.name(),
                candidate.skills(),
                candidate.educationLevel(),
                candidate.totalExperienceYears(),
                candidate.skills()
        );
    }
}
