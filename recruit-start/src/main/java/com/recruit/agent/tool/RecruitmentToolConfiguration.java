package com.recruit.agent.tool;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.domain.JobRepository;
import com.recruit.agent.knowledge.application.KnowledgeApplicationService;
import com.recruit.agent.trace.AgentTraceToolTraceSink;
import com.recruit.agent.trace.ToolTraceSink;
import com.recruit.agent.trace.UnifiedTraceRecorder;
import com.recruit.agent.vector.domain.KnowledgeChunkMatch;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class RecruitmentToolConfiguration {

    public static final String READ_PERMISSION = "recruit.read";

    @Bean
    public ToolRegistry recruitmentToolRegistry(JobRepository jobs, CandidateRepository candidates,
                                                KnowledgeApplicationService knowledge) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(descriptor("get_job_info", "Read the current recruitment job",
                        List.of(requiredLong("jobId"))),
                (context, arguments) -> jobData(requireJob(jobs, longArgument(arguments, "jobId"))));
        registry.register(descriptor("get_candidate_status", "Read the current candidate status",
                        List.of(requiredLong("candidateId"))),
                (context, arguments) -> candidateData(requireCandidate(candidates,
                        longArgument(arguments, "candidateId"))));
        registry.register(descriptor("search_knowledge", "Search recruitment policies and FAQs",
                        List.of(new ToolParameter("query", ToolParameterType.STRING, true, "Search query"),
                                new ToolParameter("limit", ToolParameterType.INTEGER, false, "Maximum results"))),
                (context, arguments) -> knowledgeData(knowledge.search(
                        String.valueOf(arguments.get("query")), intArgument(arguments, "limit", 3))));
        return registry;
    }

    @Bean
    public ToolTraceSink toolTraceSink(UnifiedTraceRecorder traceRecorder) {
        return new AgentTraceToolTraceSink(traceRecorder);
    }

    @Bean(destroyMethod = "close")
    public ToolRuntime recruitmentToolRuntime(ToolRegistry registry,
                                              ToolIdempotencyStore idempotencyStore,
                                              ToolTraceSink traceSink) {
        return new ToolRuntime(registry, new DefaultToolPermissionGuard(), idempotencyStore, traceSink);
    }

    private ToolDescriptor descriptor(String name, String description, List<ToolParameter> parameters) {
        return new ToolDescriptor(name, description, new ToolParameterContract(parameters, true),
                Set.of(READ_PERMISSION), Duration.ofSeconds(3), 1, false);
    }

    private ToolParameter requiredLong(String name) {
        return new ToolParameter(name, ToolParameterType.LONG, true, name);
    }

    private long longArgument(Map<String, Object> arguments, String name) {
        return ((Number) arguments.get(name)).longValue();
    }

    private int intArgument(Map<String, Object> arguments, String name, int defaultValue) {
        Object value = arguments.get(name);
        return value instanceof Number number ? Math.max(1, Math.min(number.intValue(), 10)) : defaultValue;
    }

    private Job requireJob(JobRepository repository, long id) throws ToolExecutionException {
        return repository.findById(id).orElseThrow(() ->
                new ToolExecutionException(ToolErrorCode.EXECUTION_FAILED, "Job not found: " + id, false));
    }

    private Candidate requireCandidate(CandidateRepository repository, long id) throws ToolExecutionException {
        return repository.findById(id).orElseThrow(() ->
                new ToolExecutionException(ToolErrorCode.EXECUTION_FAILED, "Candidate not found: " + id, false));
    }

    private Map<String, Object> jobData(Job job) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", job.id());
        data.put("name", job.name());
        data.put("description", job.description());
        data.put("requiredSkills", job.requiredSkills());
        data.put("preferredSkills", job.preferredSkills());
        data.put("status", job.status().name());
        return Map.copyOf(data);
    }

    private Map<String, Object> candidateData(Candidate candidate) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", candidate.id());
        data.put("name", candidate.name());
        data.put("status", candidate.status().name());
        data.put("expectedCity", candidate.expectedCity() == null ? "" : candidate.expectedCity());
        return Map.copyOf(data);
    }

    private List<Map<String, Object>> knowledgeData(List<KnowledgeChunkMatch> matches) {
        return matches.stream().map(match -> Map.<String, Object>of(
                "documentId", match.documentId(),
                "chunkId", match.chunkId(),
                "source", match.sourceName(),
                "content", match.content(),
                "score", match.score()
        )).toList();
    }
}
