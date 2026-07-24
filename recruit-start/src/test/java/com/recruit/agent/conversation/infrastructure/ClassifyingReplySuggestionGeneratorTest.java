package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationStage;
import com.recruit.agent.conversation.domain.ConversationStatus;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.conversation.domain.ReplyIntent;
import com.recruit.agent.conversation.domain.ReplySuggestionDraft;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ClassifyingReplySuggestionGeneratorTest {

    @Test
    void preservesReplyTextButAppliesSeparatedClassificationAndHandoff() {
        AtomicReference<List<ConversationMessage>> delegatedMessages = new AtomicReference<>();
        ReplySuggestionGenerator delegate = (job, candidate, conversation, messages) -> {
            delegatedMessages.set(messages);
            return new ReplySuggestionDraft(ReplyIntent.UNKNOWN, 0.1, false, "请等待 HR 确认。");
        };
        var generator = new ClassifyingReplySuggestionGenerator(
                delegate,
                new BudgetedConversationMemoryAssembler(512, 2,
                        (conversation, messages, maximumTokens) -> "早期对话摘要"),
                new RuleBasedConversationIntentClassifier(),
                new DeterministicConversationRoutingPolicy()
        );
        List<ConversationMessage> messages = List.of(
                message(1L, "你好"),
                message(2L, "想了解岗位"),
                message(3L, "薪资可以保证三万吗？")
        );

        ReplySuggestionDraft draft = generator.generate(job(), candidate(), conversation(), messages);

        assertThat(draft.intent()).isEqualTo(ReplyIntent.ASK_SALARY);
        assertThat(draft.needHuman()).isTrue();
        assertThat(draft.suggestedReply()).isEqualTo("请等待 HR 确认。");
        assertThat(delegatedMessages.get()).hasSize(3);
        assertThat(delegatedMessages.get().get(0).senderType()).isEqualTo(MessageSenderType.SYSTEM);
        assertThat(delegatedMessages.get().get(0).content()).contains("candidateName=张三", "早期对话摘要");
    }

    @Test
    void preservesDelegateClassificationWhenNoDeterministicRuleMatches() {
        ReplySuggestionGenerator delegate = (job, candidate, conversation, messages) ->
                new ReplySuggestionDraft(ReplyIntent.ASK_INTENTION, 0.83, false, "想了解您的求职意向。");
        var generator = new ClassifyingReplySuggestionGenerator(
                delegate,
                new BudgetedConversationMemoryAssembler(),
                new RuleBasedConversationIntentClassifier(),
                new DeterministicConversationRoutingPolicy()
        );

        ReplySuggestionDraft draft = generator.generate(job(), candidate(), conversation(),
                List.of(message(1L, "最近有考虑新的机会")));

        assertThat(draft.intent()).isEqualTo(ReplyIntent.ASK_INTENTION);
        assertThat(draft.confidence()).isEqualTo(0.83);
        assertThat(draft.needHuman()).isFalse();
    }

    private ConversationMessage message(Long id, String content) {
        return new ConversationMessage(id, 3L, MessageSenderType.CANDIDATE, content, Instant.now());
    }

    private Job job() {
        return new Job(1L, "Java", "Backend", Set.of("java"), Set.of(),
                EducationLevel.BACHELOR, 1, 3, Instant.EPOCH);
    }

    private Candidate candidate() {
        return new Candidate(2L, "张三", "p", "e", EducationLevel.BACHELOR, 2,
                "成都", Set.of("java"), Instant.EPOCH);
    }

    private Conversation conversation() {
        return new Conversation(3L, 1L, 2L, ConversationStage.INITIAL_CONTACT,
                ConversationStatus.OPEN, Instant.EPOCH);
    }
}
