package com.recruit.agent.candidate.domain;

public interface DetailedResumeProfileExtractor extends ResumeProfileExtractor {

    ResumeProfileExtractionResult extractDetailed(String rawText);

    @Override
    default CandidateProfileDraft extract(String rawText) {
        return extractDetailed(rawText).profile();
    }
}
