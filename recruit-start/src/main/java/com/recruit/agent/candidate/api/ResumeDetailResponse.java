package com.recruit.agent.candidate.api;

public record ResumeDetailResponse(
        ResumeResponse resume,
        ResumeStructuredDetail structuredDetail
) {
}
