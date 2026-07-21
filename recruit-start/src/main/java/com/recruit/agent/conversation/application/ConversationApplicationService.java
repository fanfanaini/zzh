package com.recruit.agent.conversation.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationMessageRepository;
import com.recruit.agent.conversation.domain.ConversationRepository;
import com.recruit.agent.conversation.domain.ConversationStatus;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.conversation.domain.ReplySuggestion;
import com.recruit.agent.conversation.domain.ReplySuggestionDraft;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.conversation.domain.ReplySuggestionRepository;
import com.recruit.agent.conversation.domain.ReplySuggestionStatus;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.domain.JobRepository;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

public class ConversationApplicationService {

    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ReplySuggestionRepository suggestionRepository;
    private final ReplySuggestionGenerator generator;

    public ConversationApplicationService(
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ReplySuggestionRepository suggestionRepository,
            ReplySuggestionGenerator generator
    ) {
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.suggestionRepository = suggestionRepository;
        this.generator = generator;
    }

    public Conversation create(CreateConversationCommand command) {
        requireJob(command.jobId());
        requireCandidate(command.candidateId());
        return conversationRepository.save(new Conversation(
                conversationRepository.nextConversationId(),
                command.jobId(),
                command.candidateId(),
                command.stage(),
                ConversationStatus.OPEN,
                Instant.now()
        ));
    }

    public Conversation get(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("会话不存在: " + id));
    }

    public List<ConversationMessage> listMessages(Long conversationId) {
        get(conversationId);
        return messageRepository.findByConversationId(conversationId);
    }

    public List<Conversation> list(Long jobId, Long candidateId, int limit) {
        List<Conversation> conversations;
        if (jobId != null) {
            conversations = conversationRepository.findByJobId(jobId);
        } else if (candidateId != null) {
            conversations = conversationRepository.findByCandidateId(candidateId);
        } else {
            conversations = conversationRepository.findAll();
        }
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return conversations.stream().limit(safeLimit).toList();
    }

    public ConversationMessage addMessage(AddMessageCommand command) {
        get(command.conversationId());
        return messageRepository.save(new ConversationMessage(
                messageRepository.nextMessageId(),
                command.conversationId(),
                command.senderType(),
                command.content(),
                Instant.now()
        ));
    }

    public ReplySuggestion generateReply(Long conversationId) {
        Conversation conversation = get(conversationId);
        Job job = requireJob(conversation.jobId());
        Candidate candidate = requireCandidate(conversation.candidateId());
        ReplySuggestionDraft draft = generator.generate(
                job,
                candidate,
                conversation,
                messageRepository.findByConversationId(conversationId)
        );
        return suggestionRepository.save(new ReplySuggestion(
                suggestionRepository.nextSuggestionId(),
                conversationId,
                draft.intent(),
                draft.confidence(),
                draft.needHuman(),
                draft.suggestedReply(),
                ReplySuggestionStatus.DRAFT,
                Instant.now()
        ));
    }

    public ReplySuggestion approveReply(Long suggestionId) {
        ReplySuggestion existing = getSuggestion(suggestionId);
        ReplySuggestion suggestion = suggestionRepository.save(existing.approve());
        if (existing.status() != ReplySuggestionStatus.APPROVED) {
            messageRepository.save(new ConversationMessage(
                    messageRepository.nextMessageId(),
                    suggestion.conversationId(),
                    MessageSenderType.HR,
                    suggestion.suggestedReply(),
                    Instant.now()
            ));
        }
        return suggestion;
    }

    public ReplySuggestion rejectReply(Long suggestionId) {
        ReplySuggestion suggestion = getSuggestion(suggestionId).reject();
        return suggestionRepository.save(suggestion);
    }

    public List<ReplySuggestion> listReplySuggestions(Long conversationId) {
        get(conversationId);
        return suggestionRepository.findByConversationId(conversationId);
    }

    public Conversation updateStage(Long conversationId, com.recruit.agent.conversation.domain.ConversationStage stage) {
        return conversationRepository.save(get(conversationId).updateStage(stage));
    }

    public Conversation close(Long conversationId) {
        return conversationRepository.save(get(conversationId).close());
    }

    public Conversation markCandidateReplied(Long conversationId) {
        return conversationRepository.save(get(conversationId).markCandidateReplied());
    }

    public Conversation scheduleFollowUp(Long conversationId, Instant nextFollowUpAt) {
        return conversationRepository.save(get(conversationId).scheduleFollowUp(nextFollowUpAt));
    }

    private ReplySuggestion getSuggestion(Long suggestionId) {
        return suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new NoSuchElementException("回复建议不存在: " + suggestionId));
    }

    private Job requireJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("岗位不存在: " + jobId));
    }

    private Candidate requireCandidate(Long candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new NoSuchElementException("候选人不存在: " + candidateId));
    }
}
