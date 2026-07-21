package com.recruit.agent.candidate.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.candidate.domain.CandidateStatus;
import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeParseStatus;
import com.recruit.agent.candidate.domain.ResumeRepository;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

public class CandidateApplicationService {

    private final CandidateRepository candidateRepository;
    private final ResumeRepository resumeRepository;

    public CandidateApplicationService(CandidateRepository candidateRepository, ResumeRepository resumeRepository) {
        this.candidateRepository = candidateRepository;
        this.resumeRepository = resumeRepository;
    }

    public Candidate register(RegisterCandidateCommand command) {
        Candidate candidate = new Candidate(
                candidateRepository.nextCandidateId(),
                command.name(),
                command.phoneHash(),
                command.emailHash(),
                command.educationLevel(),
                command.totalExperienceYears(),
                command.expectedCity(),
                command.skills(),
                Instant.now()
        );
        return candidateRepository.save(candidate);
    }

    public Candidate get(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("候选人不存在: " + id));
    }

    public List<Candidate> list() {
        return search(null, null);
    }

    public List<Candidate> search(String keyword, CandidateStatus status) {
        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase();
        return candidateRepository.findAll().stream()
                .filter(candidate -> candidate.status() != CandidateStatus.DELETED)
                .filter(candidate -> status == null || candidate.status() == status)
                .filter(candidate -> normalizedKeyword == null || normalizedKeyword.isBlank()
                        || candidate.name().toLowerCase().contains(normalizedKeyword)
                        || candidate.skills().stream().anyMatch(skill -> skill.contains(normalizedKeyword)))
                .toList();
    }

    public Candidate update(Long id, RegisterCandidateCommand command) {
        Candidate existing = get(id);
        return candidateRepository.save(existing.update(command.name(), command.phoneHash(), command.emailHash(),
                command.educationLevel(), command.totalExperienceYears(), command.expectedCity(), command.skills()));
    }

    public Candidate disable(Long id) {
        return candidateRepository.save(get(id).disable());
    }

    public Candidate delete(Long id) {
        return candidateRepository.save(get(id).markDeleted());
    }

    public ResumeDocument addResume(AddResumeCommand command) {
        get(command.candidateId());
        ResumeDocument resume = new ResumeDocument(
                resumeRepository.nextResumeId(),
                command.candidateId(),
                command.originalFilename(),
                command.contentType(),
                command.fileSize(),
                command.storageObjectName(),
                ResumeParseStatus.UPLOADED,
                null,
                Instant.now()
        );
        return resumeRepository.save(resume);
    }

    public List<ResumeDocument> listResumes(Long candidateId) {
        get(candidateId);
        return resumeRepository.findByCandidateId(candidateId);
    }

    public ResumeDocument getResume(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NoSuchElementException("Resume not found: " + resumeId));
    }

    public ResumeDocument deleteResume(Long resumeId) {
        ResumeDocument resume = getResume(resumeId);
        resumeRepository.deleteById(resumeId);
        return resume;
    }

    public ResumeDocument updateResumeStructuredDetail(Long resumeId, String structuredDetailJson) {
        ResumeDocument resume = getResume(resumeId);
        return resumeRepository.save(resume.withStructuredDetailJson(structuredDetailJson));
    }
}
