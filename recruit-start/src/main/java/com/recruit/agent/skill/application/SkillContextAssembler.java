package com.recruit.agent.skill.application;

import com.recruit.agent.skill.domain.RecruitmentSkillRepository;
import com.recruit.agent.skill.domain.RecruitmentSkillVersion;
import com.recruit.agent.trace.AgentTraceStatus;
import com.recruit.agent.trace.TraceComponent;
import com.recruit.agent.trace.UnifiedTraceRecorder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkillContextAssembler {
    private final RecruitmentSkillRepository repository;
    private final RecruitmentSkillMatcher matcher;
    private final RecruitmentSkillProperties properties;
    private final UnifiedTraceRecorder traceRecorder;

    public SkillContextAssembler(RecruitmentSkillRepository repository,
                                 RecruitmentSkillMatcher matcher,
                                 RecruitmentSkillProperties properties,
                                 UnifiedTraceRecorder traceRecorder) {
        this.repository = repository;
        this.matcher = matcher;
        this.properties = properties;
        this.traceRecorder = traceRecorder;
    }

    public SkillContext assemble(String query) {
        if (!properties.isEnabled() || query == null || query.isBlank()) {
            return SkillContext.empty();
        }
        try (UnifiedTraceRecorder.Span span = traceRecorder == null ? null
                : traceRecorder.start(TraceComponent.SKILL, "assemble_context", Map.of("queryLength", query.length()))) {
            List<SkillMatch> matches = matcher.match(query, repository.enabledCards(),
                    properties.getMatchLimit(), properties.getMinScore());
            if (matches.isEmpty()) {
                if (span != null) span.succeed(Map.of("matches", 0));
                return SkillContext.empty();
            }
            StringBuilder context = new StringBuilder();
            Set<String> tools = new LinkedHashSet<>();
            Set<String> risks = new LinkedHashSet<>();
            for (SkillMatch match : matches) {
                RecruitmentSkillVersion version = repository.findCurrentVersion(match.skillId()).orElse(null);
                if (version == null) continue;
                tools.addAll(version.allowedTools());
                risks.addAll(version.riskTags());
                appendVersion(context, match, version);
                if (context.length() >= properties.getMaxPromptChars()) {
                    break;
                }
            }
            String promptContext = truncate(context.toString(), properties.getMaxPromptChars());
            if (span != null) {
                span.succeed(Map.of("matches", matches.size(),
                        "skillIds", matches.stream().map(SkillMatch::skillId).toList(),
                        "versions", matches.stream().map(SkillMatch::version).toList(),
                        "contextChars", promptContext.length()));
            }
            return new SkillContext(promptContext, matches, List.copyOf(tools), List.copyOf(risks));
        } catch (RuntimeException exception) {
            if (traceRecorder != null) {
                traceRecorder.start(TraceComponent.SKILL, "assemble_context_error",
                        Map.of("error", exception.getClass().getSimpleName())).succeed(Map.of());
            }
            return SkillContext.empty();
        }
    }

    private void appendVersion(StringBuilder context, SkillMatch match, RecruitmentSkillVersion version) {
        context.append("Skill: ").append(match.name())
                .append(" | category: ").append(match.category())
                .append(" | version: ").append(version.version())
                .append(" | matchScore: ").append(Math.round(match.score()))
                .append(" | reason: ").append(match.reason())
                .append('\n');
        context.append("Instruction:\n").append(version.instruction()).append('\n');
        if (!version.promptFragment().isBlank()) {
            context.append("Prompt fragment:\n").append(version.promptFragment()).append('\n');
        }
        if (!version.guardrails().isBlank()) {
            context.append("Guardrails:\n").append(version.guardrails()).append('\n');
        }
        if (!version.fewShots().isBlank()) {
            context.append("Few-shot examples:\n").append(version.fewShots()).append('\n');
        }
        if (!version.handoffPolicy().isBlank()) {
            context.append("Human handoff policy:\n").append(version.handoffPolicy()).append('\n');
        }
        context.append('\n');
    }

    private String truncate(String value, int maxChars) {
        int safeMax = Math.max(1000, maxChars);
        return value.length() <= safeMax ? value : value.substring(0, safeMax) + "\n[skill context truncated]";
    }
}
