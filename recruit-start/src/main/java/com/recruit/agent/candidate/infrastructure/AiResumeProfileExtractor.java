package com.recruit.agent.candidate.infrastructure;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.domain.LlmStructuredRequest;
import com.recruit.agent.candidate.domain.CandidateProfileDraft;
import com.recruit.agent.candidate.domain.DetailedResumeProfileExtractor;
import com.recruit.agent.candidate.domain.ResumeProfileExtractionResult;
import com.recruit.agent.candidate.domain.ResumeProfileExtractor;
import com.recruit.agent.screening.domain.EducationLevel;

import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Set;

public class AiResumeProfileExtractor implements DetailedResumeProfileExtractor {

    private final LlmService llmService;
    private final ResumeProfileExtractor fallback;

    public AiResumeProfileExtractor(LlmService llmService, ResumeProfileExtractor fallback) {
        this.llmService = llmService;
        this.fallback = fallback;
    }

    @Override
    public ResumeProfileExtractionResult extractDetailed(String rawText) {
        try {
            AiResumeProfileResult result = llmService.structuredChat(new LlmStructuredRequest<>(
                    "RESUME_PROFILE_EXTRACTION",
                    "resume-profile-v2",
                    """
                            你是招聘系统的简历结构化解析助手。
                            只根据简历原文输出 JSON，不要编造信息。
                            educationLevel 只能是 UNKNOWN, COLLEGE, BACHELOR, MASTER, DOCTOR。
                            skills 输出标准化小写技能名数组。
                            每段教育、工作和项目经历都必须带 sourceText，confidence 范围为 0 到 1。
                            fieldSources 保存关键扁平字段对应的原文证据，fieldConfidence 保存其置信度。
                            原文没有的信息输出空数组、空对象或 null。
                            """,
                    """
                            请从下面简历原文中抽取候选人画像，输出 JSON：
                            {
                              "educationLevel": "BACHELOR",
                              "totalExperienceYears": 2.5,
                              "expectedCity": "成都",
                              "skills": ["java", "spring boot", "mysql"],
                              "educationExperiences": [
                                {
                                  "school": "示例大学",
                                  "degree": "本科",
                                  "major": "软件工程",
                                  "startDate": "2018-09",
                                  "endDate": "2022-06",
                                  "sourceText": "原文证据",
                                  "confidence": 0.95
                                }
                              ],
                              "workExperiences": [],
                              "projectExperiences": [],
                              "certificates": [],
                              "languages": [],
                              "awards": [],
                              "fieldSources": {
                                "educationLevel": "原文证据"
                              },
                              "fieldConfidence": {
                                "educationLevel": 0.95
                              }
                            }

                            简历原文：
                            %s
                            """.formatted(rawText),
                    AiResumeProfileResult.class
            ));
            CandidateProfileDraft fallbackProfile = fallback.extract(rawText);
            EducationLevel aiEducation = parseEducation(result.educationLevel());
            CandidateProfileDraft profile = new CandidateProfileDraft(
                    higherEducation(aiEducation, fallbackProfile.educationLevel()),
                    result.totalExperienceYears() > 0
                            ? result.totalExperienceYears()
                            : fallbackProfile.totalExperienceYears(),
                    result.expectedCity() == null || result.expectedCity().isBlank()
                            ? fallbackProfile.expectedCity()
                            : result.expectedCity(),
                    mergeSkills(result.skills(), fallbackProfile.skills())
            );
            return new ResumeProfileExtractionResult(profile, toStructuredDetail(result));
        } catch (RuntimeException exception) {
            return new ResumeProfileExtractionResult(fallback.extract(rawText),
                    ResumeProfileExtractionResult.StructuredDetail.empty());
        }
    }

    private ResumeProfileExtractionResult.StructuredDetail toStructuredDetail(AiResumeProfileResult result) {
        return new ResumeProfileExtractionResult.StructuredDetail(
                map(result.educationExperiences(), value -> new ResumeProfileExtractionResult.EducationExperience(
                        value.school(), value.degree(), value.major(), value.startDate(), value.endDate(),
                        value.sourceText(), clampNullable(value.confidence()))),
                map(result.workExperiences(), value -> new ResumeProfileExtractionResult.WorkExperience(
                        value.company(), value.title(), value.startDate(), value.endDate(), value.description(),
                        value.sourceText(), clampNullable(value.confidence()))),
                map(result.projectExperiences(), value -> new ResumeProfileExtractionResult.ProjectExperience(
                        value.name(), value.role(), value.startDate(), value.endDate(), value.description(),
                        value.sourceText(), clampNullable(value.confidence()))),
                normalizeLabels(result.certificates()),
                normalizeLabels(result.languages()),
                normalizeLabels(result.awards()),
                normalizeSources(result.fieldSources()),
                normalizeConfidence(result.fieldConfidence())
        );
    }

    private Set<String> mergeSkills(java.util.List<String> aiSkills, Set<String> fallbackSkills) {
        Set<String> merged = new LinkedHashSet<>();
        if (aiSkills != null) {
            aiSkills.stream().filter(value -> value != null && !value.isBlank())
                    .map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .forEach(merged::add);
        }
        merged.addAll(fallbackSkills);
        return Set.copyOf(merged);
    }

    private java.util.List<String> normalizeLabels(java.util.List<?> values) {
        if (values == null) {
            return java.util.List.of();
        }
        return values.stream()
                .map(this::normalizeLabel)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeLabel(Object value) {
        if (value instanceof String text) {
            return text.trim();
        }
        if (!(value instanceof java.util.Map<?, ?> map)) {
            return value == null ? null : value.toString();
        }
        String name = firstText(map, "name", "title", "language", "certificate", "award");
        String level = firstText(map, "proficiency", "level");
        if (name == null) {
            return level;
        }
        return level == null || name.equalsIgnoreCase(level) ? name : name + " (" + level + ")";
    }

    private String firstText(java.util.Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private java.util.Map<String, String> normalizeSources(java.util.Map<String, String> values) {
        if (values == null) {
            return java.util.Map.of();
        }
        return values.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        java.util.Map.Entry::getKey,
                        java.util.Map.Entry::getValue
                ));
    }

    private EducationLevel higherEducation(EducationLevel first, EducationLevel second) {
        return educationRank(first) >= educationRank(second) ? first : second;
    }

    private int educationRank(EducationLevel level) {
        return switch (level) {
            case UNKNOWN -> 0;
            case HIGH_SCHOOL -> 1;
            case COLLEGE -> 2;
            case BACHELOR -> 3;
            case MASTER -> 4;
            case DOCTOR -> 5;
        };
    }

    private <T, R> java.util.List<R> map(java.util.List<T> values, java.util.function.Function<T, R> mapper) {
        return values == null ? java.util.List.of() : values.stream().map(mapper).toList();
    }

    private java.util.Map<String, Double> normalizeConfidence(java.util.Map<String, Double> values) {
        if (values == null) {
            return java.util.Map.of();
        }
        return values.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        java.util.Map.Entry::getKey,
                        entry -> clamp(entry.getValue())
                ));
    }

    private Double clampNullable(Double value) {
        return value == null ? null : clamp(value);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private EducationLevel parseEducation(String value) {
        if (value == null || value.isBlank()) {
            return EducationLevel.UNKNOWN;
        }
        try {
            return EducationLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return EducationLevel.UNKNOWN;
        }
    }
}
