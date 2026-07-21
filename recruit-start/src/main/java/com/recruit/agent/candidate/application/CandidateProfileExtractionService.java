package com.recruit.agent.candidate.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.CandidateProfileDraft;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.candidate.domain.DetailedResumeProfileExtractor;
import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeProfileExtractionResult;
import com.recruit.agent.candidate.domain.ResumeProfileExtractor;
import com.recruit.agent.candidate.domain.ResumeRepository;

import java.util.NoSuchElementException;

public class CandidateProfileExtractionService {

    private final CandidateRepository candidateRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeProfileExtractor profileExtractor;
    private final ObjectMapper objectMapper;

    public CandidateProfileExtractionService(
            CandidateRepository candidateRepository,
            ResumeRepository resumeRepository,
            ResumeProfileExtractor profileExtractor
    ) {
        this(candidateRepository, resumeRepository, profileExtractor, new ObjectMapper());
    }

    public CandidateProfileExtractionService(
            CandidateRepository candidateRepository,
            ResumeRepository resumeRepository,
            ResumeProfileExtractor profileExtractor,
            ObjectMapper objectMapper
    ) {
        this.candidateRepository = candidateRepository;
        this.resumeRepository = resumeRepository;
        this.profileExtractor = profileExtractor;
        this.objectMapper = objectMapper;
    }

    public Candidate extractProfile(Long resumeId) {
        ResumeDocument resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NoSuchElementException("简历不存在: " + resumeId));
        if (resume.rawText() == null || resume.rawText().isBlank()) {
            throw new IllegalStateException("简历尚未完成文本提取");
        }
        Candidate candidate = candidateRepository.findById(resume.candidateId())
                .orElseThrow(() -> new NoSuchElementException("候选人不存在: " + resume.candidateId()));
        CandidateProfileDraft draft;
        if (profileExtractor instanceof DetailedResumeProfileExtractor detailedExtractor) {
            ResumeProfileExtractionResult result = detailedExtractor.extractDetailed(resume.rawText());
            draft = result.profile();
            resumeRepository.save(resume.withStructuredDetailJson(writeStructuredDetail(result.structuredDetail())));
        } else {
            draft = profileExtractor.extract(resume.rawText());
        }
        return candidateRepository.save(candidate.withProfile(draft));
    }

    private String writeStructuredDetail(ResumeProfileExtractionResult.StructuredDetail detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("简历结构化详情保存失败", exception);
        }
    }
}
