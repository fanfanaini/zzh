package com.recruit.agent.candidate.domain;

public interface ResumeProfileExtractor {

    CandidateProfileDraft extract(String rawText);
}
