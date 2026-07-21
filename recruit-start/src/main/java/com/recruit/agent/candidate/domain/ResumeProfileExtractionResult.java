package com.recruit.agent.candidate.domain;

import java.util.List;
import java.util.Map;

public record ResumeProfileExtractionResult(
        CandidateProfileDraft profile,
        StructuredDetail structuredDetail
) {
    public ResumeProfileExtractionResult {
        if (profile == null) {
            throw new IllegalArgumentException("候选人画像不能为空");
        }
        if (structuredDetail == null) {
            structuredDetail = StructuredDetail.empty();
        }
    }

    public record StructuredDetail(
            List<EducationExperience> educationExperiences,
            List<WorkExperience> workExperiences,
            List<ProjectExperience> projectExperiences,
            List<String> certificates,
            List<String> languages,
            List<String> awards,
            Map<String, String> fieldSources,
            Map<String, Double> fieldConfidence
    ) {
        public StructuredDetail {
            educationExperiences = immutable(educationExperiences);
            workExperiences = immutable(workExperiences);
            projectExperiences = immutable(projectExperiences);
            certificates = immutable(certificates);
            languages = immutable(languages);
            awards = immutable(awards);
            fieldSources = fieldSources == null ? Map.of() : Map.copyOf(fieldSources);
            fieldConfidence = fieldConfidence == null ? Map.of() : Map.copyOf(fieldConfidence);
        }

        public static StructuredDetail empty() {
            return new StructuredDetail(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    Map.of(), Map.of());
        }

        private static <T> List<T> immutable(List<T> values) {
            return values == null ? List.of() : List.copyOf(values);
        }
    }

    public record EducationExperience(
            String school,
            String degree,
            String major,
            String startDate,
            String endDate,
            String sourceText,
            Double confidence
    ) {
    }

    public record WorkExperience(
            String company,
            String title,
            String startDate,
            String endDate,
            String description,
            String sourceText,
            Double confidence
    ) {
    }

    public record ProjectExperience(
            String name,
            String role,
            String startDate,
            String endDate,
            String description,
            String sourceText,
            Double confidence
    ) {
    }
}
