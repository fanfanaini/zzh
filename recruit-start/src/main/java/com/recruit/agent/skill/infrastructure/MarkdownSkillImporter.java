package com.recruit.agent.skill.infrastructure;

import com.recruit.agent.skill.application.CreateRecruitmentSkillCommand;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MarkdownSkillImporter {

    public CreateRecruitmentSkillCommand parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            throw new IllegalArgumentException("Skill markdown must not be blank");
        }
        ParsedMarkdown parsed = parseMarkdown(markdown);
        Map<String, String> meta = parsed.metadata();
        return new CreateRecruitmentSkillCommand(
                required(meta, "name"),
                meta.getOrDefault("category", "recruitment"),
                meta.getOrDefault("description", firstParagraph(parsed.body())),
                meta.getOrDefault("scenario", meta.getOrDefault("description", firstParagraph(parsed.body()))),
                csv(meta.getOrDefault("keywords", "")),
                intValue(meta.get("priority"), 0),
                meta.getOrDefault("version", "v1"),
                section(parsed.body(), "Instruction", parsed.body()),
                section(parsed.body(), "Prompt Fragment", ""),
                section(parsed.body(), "Few Shots", ""),
                section(parsed.body(), "Guardrails", ""),
                csv(meta.getOrDefault("allowedTools", meta.getOrDefault("allowed-tools", ""))),
                csv(meta.getOrDefault("riskTags", meta.getOrDefault("risk-tags", ""))),
                section(parsed.body(), "Handoff Policy", ""),
                Boolean.parseBoolean(meta.getOrDefault("publish", "true"))
        );
    }

    private ParsedMarkdown parseMarkdown(String markdown) {
        String normalized = markdown.replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) {
            return new ParsedMarkdown(Map.of(), normalized.trim());
        }
        int end = normalized.indexOf("\n---", 4);
        if (end < 0) {
            return new ParsedMarkdown(Map.of(), normalized.trim());
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        String frontMatter = normalized.substring(4, end);
        for (String line : frontMatter.split("\n")) {
            int delimiter = line.indexOf(':');
            if (delimiter > 0) {
                metadata.put(line.substring(0, delimiter).trim(),
                        trimQuotes(line.substring(delimiter + 1).trim()));
            }
        }
        return new ParsedMarkdown(metadata, normalized.substring(end + 4).trim());
    }

    private String section(String body, String heading, String fallback) {
        String marker = "## " + heading;
        int start = body.indexOf(marker);
        if (start < 0) {
            return fallback == null ? "" : fallback.trim();
        }
        int contentStart = start + marker.length();
        int next = body.indexOf("\n## ", contentStart);
        return (next < 0 ? body.substring(contentStart) : body.substring(contentStart, next)).trim();
    }

    private String required(Map<String, String> meta, String key) {
        String value = meta.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Skill markdown front matter missing: " + key);
        }
        return value;
    }

    private String firstParagraph(String body) {
        return Stream.of(body.split("\n\n"))
                .map(String::trim)
                .filter(value -> !value.isBlank() && !value.startsWith("#"))
                .findFirst()
                .orElse("Recruitment skill");
    }

    private Set<String> csv(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Stream.of(value.split(","))
                .map(String::trim)
                .map(this::trimQuotes)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private int intValue(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String trimQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private record ParsedMarkdown(Map<String, String> metadata, String body) {
    }
}
