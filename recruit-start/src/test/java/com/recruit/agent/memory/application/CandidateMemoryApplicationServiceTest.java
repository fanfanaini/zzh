package com.recruit.agent.memory.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.infrastructure.InMemoryCandidateRepository;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationStage;
import com.recruit.agent.conversation.domain.ConversationStatus;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.memory.domain.CandidateMemoryFact;
import com.recruit.agent.memory.domain.CandidateMemoryType;
import com.recruit.agent.memory.infrastructure.InMemoryCandidateMemoryRepository;
import com.recruit.agent.screening.domain.EducationLevel;
import com.recruit.agent.trace.InMemoryAgentTraceSink;
import com.recruit.agent.trace.UnifiedTraceRecorder;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateMemoryApplicationServiceTest {

    @Test
    void extractsAndUpsertsLongTermCandidateMemoryFromConversationMessages() {
        InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
        Candidate candidate = candidateRepository.save(new Candidate(candidateRepository.nextCandidateId(),
                "demo", "p", "e", EducationLevel.BACHELOR, 3, "Shanghai",
                Set.of("java"), Instant.parse("2026-07-11T00:00:00Z")));
        CandidateMemoryApplicationService service = new CandidateMemoryApplicationService(
                candidateRepository,
                new InMemoryCandidateMemoryRepository(),
                new CandidateMemoryExtractor(),
                new UnifiedTraceRecorder(new InMemoryAgentTraceSink())
        );
        Conversation conversation = new Conversation(1L, 2L, candidate.id(), ConversationStage.INITIAL_CONTACT,
                ConversationStatus.OPEN, Instant.now());

        List<CandidateMemoryFact> first = service.observeMessage(conversation,
                new ConversationMessage(10L, conversation.id(), MessageSenderType.CANDIDATE,
                        "我期望薪资 25-30k，也希望可以远程办公。", Instant.now()));
        List<CandidateMemoryFact> second = service.observeMessage(conversation,
                new ConversationMessage(11L, conversation.id(), MessageSenderType.CANDIDATE,
                        "薪资期望 28-32k，远程或者混合都可以。", Instant.now()));

        assertThat(first).extracting(CandidateMemoryFact::factKey)
                .contains("salary_expectation", "work_mode");
        assertThat(second).extracting(CandidateMemoryFact::factKey)
                .contains("salary_expectation", "work_mode");
        assertThat(service.listActive(candidate.id()))
                .filteredOn(fact -> fact.type() == CandidateMemoryType.PREFERENCE)
                .extracting(CandidateMemoryFact::factKey)
                .containsExactlyInAnyOrder("salary_expectation", "work_mode");
        assertThat(service.promptContext(candidate.id()))
                .contains("salary_expectation")
                .contains("work_mode")
                .contains("28-32k");
    }

    @Test
    void supportsManualMemoryAndDeactivate() {
        InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
        Candidate candidate = candidateRepository.save(new Candidate(candidateRepository.nextCandidateId(),
                "demo", "p", "e", EducationLevel.BACHELOR, 3, "Shanghai",
                Set.of("java"), Instant.parse("2026-07-11T00:00:00Z")));
        CandidateMemoryApplicationService service = new CandidateMemoryApplicationService(
                candidateRepository,
                new InMemoryCandidateMemoryRepository(),
                new CandidateMemoryExtractor(),
                new UnifiedTraceRecorder(new InMemoryAgentTraceSink())
        );

        CandidateMemoryFact created = service.create(new CreateCandidateMemoryCommand(candidate.id(),
                CandidateMemoryType.NOTE, "follow_up_note", "prefers phone call after 7pm",
                1.0, null, null, "manual note"));
        CandidateMemoryFact deactivated = service.deactivate(candidate.id(), created.id());

        assertThat(deactivated.active()).isFalse();
        assertThat(service.listActive(candidate.id())).isEmpty();
        assertThat(service.listAll(candidate.id())).hasSize(1);
    }
}
