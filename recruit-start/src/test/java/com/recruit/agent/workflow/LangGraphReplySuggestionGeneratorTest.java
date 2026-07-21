package com.recruit.agent.workflow;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationStage;
import com.recruit.agent.conversation.domain.ConversationStatus;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.conversation.domain.ReplyIntent;
import com.recruit.agent.conversation.domain.ReplySuggestionDraft;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.conversation.infrastructure.BudgetedConversationMemoryAssembler;
import com.recruit.agent.conversation.infrastructure.DeterministicConversationRoutingPolicy;
import com.recruit.agent.conversation.infrastructure.RuleBasedConversationIntentClassifier;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.screening.domain.EducationLevel;
import com.recruit.agent.tool.DefaultToolPermissionGuard;
import com.recruit.agent.tool.InMemoryToolIdempotencyStore;
import com.recruit.agent.tool.ToolDescriptor;
import com.recruit.agent.tool.ToolRegistry;
import com.recruit.agent.tool.ToolRuntime;
import com.recruit.agent.trace.InMemoryAgentTraceSink;
import com.recruit.agent.trace.InMemoryToolTraceSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LangGraphReplySuggestionGeneratorTest {
    private final ToolRegistry registry = new ToolRegistry();
    private final InMemoryToolTraceSink toolTraces = new InMemoryToolTraceSink();
    private final ToolRuntime tools = new ToolRuntime(registry, new DefaultToolPermissionGuard(),
            new InMemoryToolIdempotencyStore(), toolTraces);
    private final InMemoryAgentTraceSink agentTraces = new InMemoryAgentTraceSink();

    @AfterEach
    void closeRuntime() {
        tools.close();
    }

    @Test
    void safelyRejectsOutOfDomainWithoutCallingTheModel() {
        AtomicInteger calls = new AtomicInteger();
        var generator = generator((job, candidate, conversation, messages) -> {
            calls.incrementAndGet();
            return draft();
        });
        var result = generator.generate(job(), candidate(), conversation(),
                List.of(message("\u5e2e\u6211\u9884\u6d4b\u660e\u5929\u54ea\u53ea\u80a1\u7968\u4e0a\u6da8")));

        assertThat(result.intent()).isEqualTo(ReplyIntent.OUT_OF_DOMAIN);
        assertThat(result.needHuman()).isFalse();
        assertThat(calls).hasValue(0);
        assertThat(agentTraces.findByConversationId(3L)).extracting("node")
                .containsExactly("memory", "classify", "route", "safe_reply");
    }

    @Test
    void usesRegisteredToolBeforeGeneratingJobReply() {
        registry.register(new ToolDescriptor("get_job_info", "job",
                        new com.recruit.agent.tool.ToolParameterContract(List.of(
                                new com.recruit.agent.tool.ToolParameter("jobId",
                                        com.recruit.agent.tool.ToolParameterType.LONG, true, "job")), true),
                        Set.of("recruit.read"), Duration.ofSeconds(1), 1, false),
                (context, arguments) -> "verified job context");
        AtomicInteger calls = new AtomicInteger();
        ReplySuggestionGenerator delegate = (job, candidate, conversation, messages) -> {
            calls.incrementAndGet();
            assertThat(messages).anySatisfy(message ->
                    assertThat(message.content()).contains("verified job context"));
            return draft();
        };
        var generator = generator(delegate);
        var result = generator.generate(job(), candidate(), conversation(),
                List.of(message("\u8fd9\u4e2a\u5c97\u4f4d\u804c\u8d23\u548c\u6280\u672f\u6808\u662f\u4ec0\u4e48")));

        assertThat(result.intent()).isEqualTo(ReplyIntent.ANSWER_JOB_QUESTION);
        assertThat(calls).hasValue(1);
        assertThat(toolTraces.findAll()).hasSize(1);
    }

    @Test
    void forcesHumanReviewForSalaryAndUnsupportedSchedulingActions() {
        var generator = generator((job, candidate, conversation, messages) -> draft());
        var salary = generator.generate(job(), candidate(), conversation(),
                List.of(message("\u80fd\u4fdd\u8bc1\u6708\u85aa\u4e09\u4e07\u5417")));
        var scheduling = generator.generate(job(), candidate(), conversation(),
                List.of(message("\u5e2e\u6211\u9884\u7ea6\u9762\u8bd5\u65f6\u95f4")));

        assertThat(salary.needHuman()).isTrue();
        assertThat(salary.intent()).isEqualTo(ReplyIntent.ASK_SALARY);
        assertThat(scheduling.needHuman()).isTrue();
        assertThat(scheduling.suggestedReply()).contains("HR");
    }

    private LangGraphReplySuggestionGenerator generator(ReplySuggestionGenerator delegate) {
        return new LangGraphReplySuggestionGenerator(delegate, new BudgetedConversationMemoryAssembler(),
                new RuleBasedConversationIntentClassifier(), new DeterministicConversationRoutingPolicy(),
                tools, agentTraces);
    }

    private ReplySuggestionDraft draft() {
        return new ReplySuggestionDraft(ReplyIntent.UNKNOWN, 0.7, false, "draft");
    }

    private ConversationMessage message(String text) {
        return new ConversationMessage(4L, 3L, MessageSenderType.CANDIDATE, text, Instant.now());
    }

    private Job job() {
        return new Job(1L, "Java backend", "backend development", Set.of("java"), Set.of(),
                EducationLevel.BACHELOR, 1, 5, Instant.EPOCH);
    }

    private Candidate candidate() {
        return new Candidate(2L, "candidate", "p", "e", EducationLevel.BACHELOR, 2,
                "Chengdu", Set.of("java"), Instant.EPOCH);
    }

    private Conversation conversation() {
        return new Conversation(3L, 1L, 2L, ConversationStage.INITIAL_CONTACT,
                ConversationStatus.OPEN, Instant.EPOCH);
    }
}
