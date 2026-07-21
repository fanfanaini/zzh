package com.recruit.agent.candidate.infrastructure;

import com.recruit.agent.candidate.domain.CandidateProfileDraft;
import com.recruit.agent.candidate.domain.ResumeProfileExtractor;
import com.recruit.agent.screening.domain.EducationLevel;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleBasedResumeProfileExtractor implements ResumeProfileExtractor {

    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:年|years?)", Pattern.CASE_INSENSITIVE);

    private final Map<String, String> skillAliases = new LinkedHashMap<>();
    private final Set<String> cities = Set.of("北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "南京", "西安", "重庆");

    public RuleBasedResumeProfileExtractor() {
        skillAliases.put("spring boot", "spring boot");
        skillAliases.put("springboot", "spring boot");
        skillAliases.put("java", "java");
        skillAliases.put("mysql", "mysql");
        skillAliases.put("redis", "redis");
        skillAliases.put("rabbitmq", "rabbitmq");
        skillAliases.put("kafka", "kafka");
        skillAliases.put("docker", "docker");
        skillAliases.put("kubernetes", "kubernetes");
        skillAliases.put("k8s", "kubernetes");
        skillAliases.put("vue", "vue");
        skillAliases.put("typescript", "typescript");
    }

    @Override
    public CandidateProfileDraft extract(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("简历原文不能为空");
        }
        return new CandidateProfileDraft(
                extractEducation(rawText),
                extractExperienceYears(rawText),
                extractCity(rawText),
                extractSkills(rawText)
        );
    }

    private Set<String> extractSkills(String rawText) {
        String lower = rawText.toLowerCase(Locale.ROOT);
        Set<String> skills = new LinkedHashSet<>();
        for (Map.Entry<String, String> alias : skillAliases.entrySet()) {
            if (lower.contains(alias.getKey())) {
                skills.add(alias.getValue());
            }
        }
        return skills;
    }

    private double extractExperienceYears(String rawText) {
        Matcher matcher = EXPERIENCE_PATTERN.matcher(rawText);
        double max = 0;
        while (matcher.find()) {
            max = Math.max(max, Double.parseDouble(matcher.group(1)));
        }
        return max;
    }

    private String extractCity(String rawText) {
        for (String city : cities) {
            if (rawText.contains(city)) {
                return city;
            }
        }
        return null;
    }

    private EducationLevel extractEducation(String rawText) {
        if (rawText.contains("博士")) {
            return EducationLevel.DOCTOR;
        }
        if (rawText.contains("硕士") || rawText.contains("研究生")) {
            return EducationLevel.MASTER;
        }
        if (rawText.contains("本科") || rawText.toLowerCase(Locale.ROOT).contains("bachelor")) {
            return EducationLevel.BACHELOR;
        }
        if (rawText.contains("大专") || rawText.contains("专科")) {
            return EducationLevel.COLLEGE;
        }
        return EducationLevel.UNKNOWN;
    }
}
