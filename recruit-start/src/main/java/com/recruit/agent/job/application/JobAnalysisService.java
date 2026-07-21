package com.recruit.agent.job.application;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.domain.LlmStructuredRequest;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.screening.domain.EducationLevel;

import java.util.Locale;
import java.util.Set;

public class JobAnalysisService {
    private final JobApplicationService jobs;
    private final LlmService llm;

    public JobAnalysisService(JobApplicationService jobs, LlmService llm) {
        this.jobs = jobs;
        this.llm = llm;
    }

    public Job analyze(Long jobId) {
        Job job = jobs.get(jobId);
        JobAnalysisResult result = llm.structuredChat(new LlmStructuredRequest<>(
                "JOB_ANALYSIS", "job-analysis-v1",
                "你是岗位分析助手。只根据 JD 提取结构化要求，不补充 JD 中不存在的条件。输出 JSON。",
                """
                请分析下面的 JD，输出：
                {"requiredSkills":[],"preferredSkills":[],"educationRequirement":"UNKNOWN", "minExperienceYears":0,"maxExperienceYears":0}

                JD：%s
                """.formatted(job.description()), JobAnalysisResult.class));
        return jobs.update(jobId, new CreateJobCommand(job.name(), job.description(), set(result.requiredSkills()),
                set(result.preferredSkills()), education(result.educationRequirement()),
                Math.max(0, result.minExperienceYears()), Math.max(0, result.maxExperienceYears())));
    }

    private Set<String> set(java.util.List<String> values) { return values == null ? Set.of() : Set.copyOf(values); }
    private EducationLevel education(String value) {
        try { return EducationLevel.valueOf(value == null ? "UNKNOWN" : value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { return EducationLevel.UNKNOWN; }
    }

    public record JobAnalysisResult(java.util.List<String> requiredSkills, java.util.List<String> preferredSkills,
                                    String educationRequirement, double minExperienceYears, double maxExperienceYears) {}
}
