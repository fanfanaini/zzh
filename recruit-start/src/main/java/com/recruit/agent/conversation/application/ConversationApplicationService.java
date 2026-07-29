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
import com.recruit.agent.jobapplication.application.JobApplicationPipelineService;
import com.recruit.agent.memory.application.CandidateMemoryApplicationService;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class ConversationApplicationService {

    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ReplySuggestionRepository suggestionRepository;
    private final ReplySuggestionGenerator generator;
    private final CandidateMemoryApplicationService memoryService;
    private final Optional<JobApplicationPipelineService> jobApplicationPipelineService;

    public ConversationApplicationService(
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ReplySuggestionRepository suggestionRepository,
            ReplySuggestionGenerator generator,
            CandidateMemoryApplicationService memoryService
    ) {
        this(jobRepository, candidateRepository, conversationRepository, messageRepository, suggestionRepository,
                generator, memoryService, Optional.empty());
    }

    public ConversationApplicationService(
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ReplySuggestionRepository suggestionRepository,
            ReplySuggestionGenerator generator,
            CandidateMemoryApplicationService memoryService,
            Optional<JobApplicationPipelineService> jobApplicationPipelineService
    ) {
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.suggestionRepository = suggestionRepository;
        this.generator = generator;
        this.memoryService = memoryService;
        this.jobApplicationPipelineService = jobApplicationPipelineService;
    }

    public ConversationApplicationService(
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ReplySuggestionRepository suggestionRepository,
            ReplySuggestionGenerator generator
    ) {
        this(jobRepository, candidateRepository, conversationRepository, messageRepository, suggestionRepository,
                generator, null);
    }

    public Conversation create(CreateConversationCommand command) {
        requireJob(command.jobId());
        requireCandidate(command.candidateId());
        Conversation conversation = conversationRepository.save(new Conversation(
                conversationRepository.nextConversationId(),
                command.jobId(),
                command.candidateId(),
                command.stage(),
                ConversationStatus.OPEN,
                Instant.now()
        ));
        jobApplicationPipelineService.ifPresent(service ->
                service.attachConversation(conversation.jobId(), conversation.candidateId(), conversation.id()));
        return conversation;
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
        Conversation conversation = get(command.conversationId());
        ConversationMessage message = messageRepository.save(new ConversationMessage(
                messageRepository.nextMessageId(),
                command.conversationId(),
                command.senderType(),
                command.content(),
                Instant.now()
        ));
        if (memoryService != null) {
            memoryService.observeMessage(conversation, message);
        }
        return message;
    }

    public ReplySuggestion generateReply(Long conversationId) {
        Conversation conversation = get(conversationId);
        Job job = requireJob(conversation.jobId());
        Candidate candidate = requireCandidate(conversation.candidateId());
        List<ConversationMessage> messages = withCandidateLongTermMemory(
                conversation,
                messageRepository.findByConversationId(conversationId)
        );
        ReplySuggestionDraft draft = generator.generate(
                job,
                candidate,
                conversation,
                messages
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
            if (memoryService != null) {
                getConversationForSuggestion(suggestion).ifPresent(conversation ->
                        memoryService.observeMessage(conversation, new ConversationMessage(
                            Long.MIN_VALUE + suggestion.id(),
                            suggestion.conversationId(),
                            MessageSenderType.HR,
                            suggestion.suggestedReply(),
                            Instant.now()
                    )));
            }
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

    private java.util.Optional<Conversation> getConversationForSuggestion(ReplySuggestion suggestion) {
        return conversationRepository.findById(suggestion.conversationId());
    }

    private List<ConversationMessage> withCandidateLongTermMemory(Conversation conversation,
                                                                  List<ConversationMessage> messages) {
        if (memoryService == null) {
            return messages;
        }
        String memoryContext = memoryService.promptContext(conversation.candidateId());
        if (memoryContext.isBlank()) {
            return messages;
        }
        java.util.ArrayList<ConversationMessage> enriched = new java.util.ArrayList<>();
        enriched.add(new ConversationMessage(Long.MIN_VALUE + 1, conversation.id(), MessageSenderType.SYSTEM,
                "Candidate long-term memory:\n" + memoryContext, Instant.EPOCH));
        enriched.addAll(messages);
        return List.copyOf(enriched);
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
