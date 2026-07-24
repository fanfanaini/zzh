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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ContextRecoveringReplySuggestionGeneratorTest {

    @Test
    void compactsAndRetriesExactlyOnceAfterContextOverflow() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<List<ConversationMessage>> retriedMessages = new AtomicReference<>();
        ReplySuggestionGenerator primary = (job, candidate, conversation, messages) -> {
            if (calls.incrementAndGet() == 1) throw new ContextWindowExceededException("overflow", null);
            retriedMessages.set(messages);
            return draft("recovered");
        };
        AtomicInteger fallbackCalls = new AtomicInteger();
        ReplySuggestionGenerator fallback = (job, candidate, conversation, messages) -> {
            fallbackCalls.incrementAndGet();
            return draft("fallback");
        };
        HeuristicTokenEstimator estimator = new HeuristicTokenEstimator();
        var generator = new ContextRecoveringReplySuggestionGenerator(primary, fallback,
                new ConversationMessageCompactor(estimator), 32);

        var result = generator.generate(job(), candidate(), conversation(), messages());

        assertThat(result.suggestedReply()).isEqualTo("recovered");
        assertThat(calls).hasValue(2);
        assertThat(fallbackCalls).hasValue(0);
        int retryTokens = retriedMessages.get().stream()
                .mapToInt(message -> estimator.estimate(message.senderType() + ": " + message.content())).sum();
        assertThat(retryTokens).isLessThanOrEqualTo(32);
        assertThat(retriedMessages.get().size()).isLessThan(messages().size());
    }

    @Test
    void fallsBackAfterTheSingleCompactedRetryAlsoFails() {
        AtomicInteger calls = new AtomicInteger();
        ReplySuggestionGenerator primary = (job, candidate, conversation, messages) -> {
            calls.incrementAndGet();
            throw new ContextWindowExceededException("overflow", null);
        };
        var generator = new ContextRecoveringReplySuggestionGenerator(primary,
                (job, candidate, conversation, messages) -> draft("fallback"),
                new ConversationMessageCompactor(new HeuristicTokenEstimator()), 32);

        assertThat(generator.generate(job(), candidate(), conversation(), messages()).suggestedReply())
                .isEqualTo("fallback");
        assertThat(calls).hasValue(2);
    }

    private List<ConversationMessage> messages() {
        return IntStream.rangeClosed(1, 8).mapToObj(index -> new ConversationMessage((long) index, 3L,
                MessageSenderType.CANDIDATE, "message-" + index + "-" + "x".repeat(40),
                Instant.ofEpochSecond(index))).toList();
    }

    private ReplySuggestionDraft draft(String reply) {
        return new ReplySuggestionDraft(ReplyIntent.ASK_INTENTION, 0.8, false, reply);
    }

    private Job job() {
        return new Job(1L, "Java", "Backend", Set.of("java"), Set.of(),
                EducationLevel.BACHELOR, 1, 3, Instant.EPOCH);
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
