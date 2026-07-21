package com.recruit.agent.screening.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResumeScreeningService {

    public ScreeningResult screen(JobProfile job, CandidateProfile candidate) {
        return screen(job, candidate, null);
    }

    public ScreeningResult screen(JobProfile job, CandidateProfile candidate, Double semanticScore) {
        List<String> risks = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (!candidate.educationLevel().meets(job.educationRequirement())) {
            risks.add("学历未满足岗位要求");
            return notMatch(risks, missing);
        }
        if (candidate.totalExperienceYears() < job.minExperienceYears()) {
            risks.add("工作年限未满足岗位要求");
            return notMatch(risks, missing);
        }

        Set<String> missingRequiredSkills = difference(job.requiredSkills(), candidate.skills());
        if (!missingRequiredSkills.isEmpty()) {
            risks.add("缺少必须技能: " + String.join(", ", missingRequiredSkills));
            return notMatch(risks, missing);
        }

        ScoreBreakdown score = calculateScore(job, candidate, semanticScore);
        List<String> advantages = new ArrayList<>();
        if (score.skillScore() >= 90) {
            advantages.add("必须技能匹配度高");
        }
        if (score.bonusScore() > 0) {
            advantages.add("具备岗位加分技能");
        }
        if (score.projectScore() < 60) {
            missing.add("项目经历与岗位描述的语义匹配证据不足");
        }

        ScreeningRecommendation recommendation = score.finalScore() >= 80
                ? ScreeningRecommendation.INTERVIEW
                : ScreeningRecommendation.MANUAL_REVIEW;
        return new ScreeningResult(recommendation, score, advantages, risks, missing);
    }

    private ScreeningResult notMatch(List<String> risks, List<String> missing) {
        ScoreBreakdown score = new ScoreBreakdown(0, 0, 0, 0, 0, 0);
        return new ScreeningResult(ScreeningRecommendation.NOT_MATCH, score, List.of(), risks, missing);
    }

    private ScoreBreakdown calculateScore(JobProfile job, CandidateProfile candidate, Double semanticScore) {
        double skillScore = ratioScore(job.requiredSkills(), candidate.skills());
        double projectScore = semanticScore == null ? ratioScore(job.requiredSkills(), candidate.projectKeywords()) : round(semanticScore);
        double experienceScore = candidate.totalExperienceYears() >= job.minExperienceYears() ? 100 : 0;
        double educationScore = candidate.educationLevel().meets(job.educationRequirement()) ? 100 : 0;
        double bonusScore = ratioScore(job.preferredSkills(), candidate.skills());
        double finalScore = skillScore * 0.40
                + projectScore * 0.25
                + experienceScore * 0.15
                + educationScore * 0.10
                + bonusScore * 0.10;
        return new ScoreBreakdown(skillScore, projectScore, experienceScore, educationScore, bonusScore, round(finalScore));
    }

    private double ratioScore(Set<String> expected, Set<String> actual) {
        if (expected.isEmpty()) {
            return 100;
        }
        long matched = expected.stream().filter(actual::contains).count();
        return round(matched * 100.0 / expected.size());
    }

    private Set<String> difference(Set<String> expected, Set<String> actual) {
        return expected.stream()
                .filter(value -> !actual.contains(value))
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
