package com.recruit.agent.toolcalling;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.domain.LlmStructuredRequest;
import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.*;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.screening.domain.EducationLevel;
import com.recruit.agent.tool.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LlmFunctionCallingToolPlannerTest {

    @Test
    void acceptsValidModelSelectedToolCall() {
        ToolRegistry registry = registry();
        LlmFunctionCallingToolPlanner planner = new LlmFunctionCallingToolPlanner(
                new StubLlmService(new LlmToolCallDecisionResult(true, "get_job_info",
                        Map.of("jobId", 100L), 0.91, "job details are needed")),
                registry);

        ToolCallDecision decision = planner.decide(request(List.of("get_job_info")));

        assertThat(decision.callTool()).isTrue();
        assertThat(decision.toolName()).isEqualTo("get_job_info");
        assertThat(decision.arguments()).containsEntry("jobId", 100L);
        assertThat(decision.confidence()).isEqualTo(0.91);
    }

    @Test
    void rejectsUnavailableToolSelectedByModel() {
        ToolRegistry registry = registry();
        LlmFunctionCallingToolPlanner planner = new LlmFunctionCallingToolPlanner(
                new StubLlmService(new LlmToolCallDecisionResult(true, "delete_candidate",
                        Map.of("candidateId", 1L), 0.8, "bad tool")),
                registry);

        ToolCallDecision decision = planner.decide(request(List.of("get_job_info")));

        assertThat(decision.callTool()).isFalse();
        assertThat(decision.reason()).contains("unavailable tool");
    }

    private ToolRegistry registry() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ToolDescriptor("get_job_info", "Read job",
                new ToolParameterContract(List.of(new ToolParameter("jobId", ToolParameterType.LONG, true, "job id")),
                        true),
                Set.of("recruit.read"), Duration.ofSeconds(1), 1, false),
                (context, arguments) -> Map.of("id", arguments.get("jobId")));
        return registry;
    }

    private ToolCallingRequest request(List<String> tools) {
        Job job = new Job(100L, "Java", "Backend role", Set.of("java"), Set.of("redis"),
                EducationLevel.BACHELOR, 1, 3, Instant.now());
        Candidate candidate = new Candidate(200L, "demo", "p", "e", EducationLevel.BACHELOR,
                2, "Shanghai", Set.of("java"), Instant.now());
        Conversation conversation = new Conversation(300L, job.id(), candidate.id(), ConversationStage.INITIAL_CONTACT,
                ConversationStatus.OPEN, Instant.now());
        ConversationMemory memory = new ConversationMemory(List.of(new ConversationMessage(1L, conversation.id(),
                MessageSenderType.CANDIDATE, "这个岗位技术栈是什么？", Instant.now())), "", Map.of(), Map.of(), 20, false);
        return new ToolCallingRequest(job, candidate, conversation, memory, ConversationRoute.JOB_INFO, tools);
    }

    private record StubLlmService(LlmToolCallDecisionResult result) implements LlmService {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T structuredChat(LlmStructuredRequest<T> request) {
            return (T) result;
        }
    }
}
