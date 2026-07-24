package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationStage;
import com.recruit.agent.conversation.domain.ConversationStatus;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetedConversationMemoryAssemblerTest {

    @Test
    void keepsNewestMessagesWithinBudgetAndSummarizesOmittedHistory() {
        AtomicInteger summarizedMessages = new AtomicInteger();
        var assembler = new BudgetedConversationMemoryAssembler(90, 3, (conversation, messages, maximumTokens) -> {
            summarizedMessages.set(messages.size());
            return "Earlier discussion about the role and availability";
        });
        List<ConversationMessage> messages = IntStream.rangeClosed(1, 6)
                .mapToObj(index -> new ConversationMessage((long) index, 3L, MessageSenderType.CANDIDATE,
                        "message-" + index + "-" + "x".repeat(20), Instant.ofEpochSecond(index)))
                .toList();

        var memory = assembler.assemble(job(), candidate(), conversation(), messages);

        assertThat(memory.tokenCount()).isLessThanOrEqualTo(90);
        assertThat(memory.truncated()).isTrue();
        assertThat(memory.recentMessages()).extracting(ConversationMessage::content)
                .containsExactly(messages.get(3).content(), messages.get(4).content(), messages.get(5).content());
        assertThat(summarizedMessages).hasValue(3);
        assertThat(memory.summary()).contains("Earlier discussion");
        assertThat(memory.facts()).containsEntry("candidateName", "张三");
        assertThat(memory.state()).containsEntry("stage", "INITIAL_CONTACT");
    }

    @Test
    void boundsOversizedFactsAsPartOfTheSameMemoryBudget() {
        var oversizedJob = new Job(1L, "J".repeat(500), "Backend", Set.of("java"), Set.of(),
                EducationLevel.BACHELOR, 1, 3, Instant.EPOCH);
        var assembler = new BudgetedConversationMemoryAssembler(64, 2,
                (conversation, messages, maximumTokens) -> "");

        var memory = assembler.assemble(oversizedJob, candidate(), conversation(), List.of());

        assertThat(memory.tokenCount()).isLessThanOrEqualTo(64);
        assertThat(memory.facts().values()).allMatch(value -> value.length() < 500);
    }

    private Job job() {
        return new Job(1L, "Java", "Backend", Set.of("java"), Set.of("redis"),
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
