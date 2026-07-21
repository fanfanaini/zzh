package com.recruit.agent.candidate.api;

import java.util.List;
import java.util.Map;

public record ResumeStructuredDetail(
        List<EducationExperience> educationExperiences,
        List<WorkExperience> workExperiences,
        List<ProjectExperience> projectExperiences,
        List<String> certificates,
        List<String> languages,
        List<String> awards,
        Map<String, String> fieldSources,
        Map<String, Double> fieldConfidence
) {
    public static ResumeStructuredDetail empty() {
        return new ResumeStructuredDetail(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), Map.of());
    }

    public record EducationExperience(String school, String degree, String major, String startDate, String endDate, String sourceText, Double confidence) {}
    public record WorkExperience(String company, String title, String startDate, String endDate, String description, String sourceText, Double confidence) {}
    public record ProjectExperience(String name, String role, String startDate, String endDate, String description, String sourceText, Double confidence) {}
}
