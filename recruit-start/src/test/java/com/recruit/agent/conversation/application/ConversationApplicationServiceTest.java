package com.recruit.agent.conversation.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.infrastructure.InMemoryCandidateRepository;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.conversation.domain.ReplySuggestion;
import com.recruit.agent.conversation.domain.ReplySuggestionStatus;
import com.recruit.agent.conversation.infrastructure.InMemoryConversationMessageRepository;
import com.recruit.agent.conversation.infrastructure.InMemoryConversationRepository;
import com.recruit.agent.conversation.infrastructure.InMemoryReplySuggestionRepository;
import com.recruit.agent.conversation.infrastructure.RuleBasedReplySuggestionGenerator;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.infrastructure.InMemoryJobRepository;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationApplicationServiceTest {

    @Test
    void createsConversationAndGeneratesReplyDraft() {
        InMemoryJobRepository jobRepository = new InMemoryJobRepository();
        InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
        Job job = jobRepository.save(javaJob(jobRepository.nextId()));
        Candidate candidate = candidateRepository.save(javaCandidate(candidateRepository.nextCandidateId()));
        ConversationApplicationService service = newService(jobRepository, candidateRepository);

        Conversation conversation = service.create(new CreateConversationCommand(job.id(), candidate.id(), null));
        service.addMessage(new AddMessageCommand(conversation.id(), MessageSenderType.CANDIDATE, "想了解一下薪资"));
        ReplySuggestion suggestion = service.generateReply(conversation.id());
        ReplySuggestion approved = service.approveReply(suggestion.id());

        assertThat(suggestion.needHuman()).isTrue();
        assertThat(suggestion.suggestedReply()).contains("薪资");
        assertThat(approved.status()).isEqualTo(ReplySuggestionStatus.APPROVED);
        assertThat(service.listMessages(conversation.id()))
                .hasSize(2)
                .last()
                .extracting("senderType", "content")
                .containsExactly(MessageSenderType.HR, suggestion.suggestedReply());
    }

    private ConversationApplicationService newService(
            InMemoryJobRepository jobRepository,
            InMemoryCandidateRepository candidateRepository
    ) {
        return new ConversationApplicationService(
                jobRepository,
                candidateRepository,
                new InMemoryConversationRepository(),
                new InMemoryConversationMessageRepository(),
                new InMemoryReplySuggestionRepository(),
                new RuleBasedReplySuggestionGenerator()
        );
    }

    private Job javaJob(Long id) {
        return new Job(id, "Java后端", "后端岗位", Set.of("java"), Set.of("redis"),
                EducationLevel.BACHELOR, 1, 3, Instant.parse("2026-07-11T00:00:00Z"));
    }

    private Candidate javaCandidate(Long id) {
        return new Candidate(id, "张三", "p", "e", EducationLevel.BACHELOR, 2,
                "成都", Set.of("java"), Instant.parse("2026-07-11T00:00:00Z"));
    }
}
