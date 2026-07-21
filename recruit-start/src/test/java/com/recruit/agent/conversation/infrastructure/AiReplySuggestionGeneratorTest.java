package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.ai.infrastructure.DefaultLlmService;
import com.recruit.agent.ai.infrastructure.InMemoryModelCallLogRepository;
import com.recruit.agent.ai.infrastructure.MockRawLlmClient;
import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationStage;
import com.recruit.agent.conversation.domain.ConversationStatus;
import com.recruit.agent.conversation.domain.ReplySuggestionDraft;
import com.recruit.agent.conversation.domain.ReplyIntent;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.screening.domain.EducationLevel;
import com.recruit.agent.knowledge.application.KnowledgeApplicationService;
import com.recruit.agent.vector.infrastructure.DeterministicEmbeddingService;
import com.recruit.agent.vector.infrastructure.InMemoryVectorSearchRepository;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.MessageSenderType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiReplySuggestionGeneratorTest {

    @Test
    void generatesReplyThroughLlm() {
        AiReplySuggestionGenerator generator = new AiReplySuggestionGenerator(
                new DefaultLlmService(new MockRawLlmClient(), new InMemoryModelCallLogRepository()),
                new RuleBasedReplySuggestionGenerator()
        );

        ReplySuggestionDraft draft = generator.generate(job(), candidate(), conversation(), List.of());

        assertThat(draft.intent()).isEqualTo(ReplyIntent.ASK_INTENTION);
        assertThat(draft.suggestedReply()).contains("工作机会");
    }

    @Test
    void retrievesKnowledgeAndAppendsSourceToReply() {
        var knowledge = new KnowledgeApplicationService(new DeterministicEmbeddingService(),
                new InMemoryVectorSearchRepository());
        knowledge.index("薪资沟通制度", "候选人询问薪资时只能说明薪资范围，具体承诺必须转人工确认。");
        AiReplySuggestionGenerator generator = new AiReplySuggestionGenerator(
                new DefaultLlmService(new MockRawLlmClient(), new InMemoryModelCallLogRepository()),
                new RuleBasedReplySuggestionGenerator(), knowledge);

        ReplySuggestionDraft draft = generator.generate(job(), candidate(), conversation(), List.of(
                new ConversationMessage(10L, 3L, MessageSenderType.CANDIDATE, "这个岗位薪资是多少？", Instant.now())
        ));

        assertThat(draft.suggestedReply()).contains("参考资料").contains("薪资沟通制度");
    }

    private Job job() {
        return new Job(1L, "Java后端", "后端开发", Set.of("java"), Set.of(),
                EducationLevel.BACHELOR, 1, 3, Instant.parse("2026-07-11T00:00:00Z"));
    }

    private Candidate candidate() {
        return new Candidate(2L, "张三", "p", "e", EducationLevel.BACHELOR, 2,
                "成都", Set.of("java"), Instant.parse("2026-07-11T00:00:00Z"));
    }

    private Conversation conversation() {
        return new Conversation(3L, 1L, 2L, ConversationStage.INITIAL_CONTACT,
                ConversationStatus.OPEN, Instant.parse("2026-07-11T00:00:00Z"));
    }
}
