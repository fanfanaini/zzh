package com.recruit.agent.skill.application;

import com.recruit.agent.skill.domain.RecruitmentSkillCard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RecruitmentSkillMatcher {

    public List<SkillMatch> match(String query, List<RecruitmentSkillCard> cards, int limit, double minScore) {
        if (query == null || query.isBlank() || cards == null || cards.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = normalize(query);
        int safeLimit = Math.max(1, Math.min(limit, 10));
        double safeMinScore = Math.max(0, Math.min(100, minScore));
        List<SkillMatch> matches = new ArrayList<>();
        for (RecruitmentSkillCard card : cards) {
            ScoredReason scored = score(normalizedQuery, card);
            if (scored.score() >= safeMinScore) {
                matches.add(new SkillMatch(card.id(), card.name(), card.category(), card.version(),
                        scored.score(), scored.reason(), card));
            }
        }
        return matches.stream()
                .sorted(Comparator.comparingDouble(SkillMatch::score).reversed()
                        .thenComparing(match -> match.card().priority(), Comparator.reverseOrder())
                        .thenComparing(SkillMatch::skillId))
                .limit(safeLimit)
                .toList();
    }

    private ScoredReason score(String query, RecruitmentSkillCard card) {
        double score = 0;
        StringBuilder reason = new StringBuilder();
        String name = normalize(card.name());
        if (!name.isBlank() && query.contains(name)) {
            score += 35;
            reason.append("name ");
        }
        String category = normalize(card.category());
        if (!category.isBlank() && query.contains(category)) {
            score += 12;
            reason.append("category ");
        }
        for (String keyword : card.triggerKeywords()) {
            String normalizedKeyword = normalize(keyword);
            if (!normalizedKeyword.isBlank() && query.contains(normalizedKeyword)) {
                score += 24;
                reason.append("keyword:").append(keyword).append(' ');
            }
        }
        for (String token : splitWords(card.description() + " " + card.scenario())) {
            if (token.length() >= 2 && query.contains(token)) {
                score += 5;
            }
        }
        return new ScoredReason(Math.min(100, score), reason.isEmpty() ? "semantic-card-overlap" : reason.toString().trim());
    }

    private List<String> splitWords(String text) {
        String normalized = normalize(text).replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", " ");
        if (normalized.isBlank()) return List.of();
        return java.util.Arrays.stream(normalized.split("\\s+"))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ScoredReason(double score, String reason) {
    }
}
