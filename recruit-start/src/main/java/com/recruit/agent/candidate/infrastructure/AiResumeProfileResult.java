package com.recruit.agent.candidate.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiResumeProfileResult(
        String educationLevel,
        double totalExperienceYears,
        String expectedCity,
        List<String> skills,
        List<EducationExperience> educationExperiences,
        List<WorkExperience> workExperiences,
        List<ProjectExperience> projectExperiences,
        List<Object> certificates,
        List<Object> languages,
        List<Object> awards,
        Map<String, String> fieldSources,
        Map<String, Double> fieldConfidence
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EducationExperience(String school, String degree, String major, String startDate, String endDate,
                                      String sourceText, Double confidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WorkExperience(String company, @JsonAlias({"position", "jobTitle"}) String title,
                                 String startDate, String endDate, String description,
                                 String sourceText, Double confidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectExperience(@JsonAlias("projectName") String name, String role,
                                    String startDate, String endDate, String description,
                                    String sourceText, Double confidence) {
    }
}
