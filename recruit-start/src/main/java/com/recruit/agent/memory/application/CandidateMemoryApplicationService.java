package com.recruit.agent.memory.application;

import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.memory.domain.CandidateMemoryFact;
import com.recruit.agent.memory.domain.CandidateMemoryRepository;
import com.recruit.agent.memory.domain.CandidateMemorySourceType;
import com.recruit.agent.trace.TraceComponent;
import com.recruit.agent.trace.UnifiedTraceRecorder;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

public class CandidateMemoryApplicationService {
    private final CandidateRepository candidateRepository;
    private final CandidateMemoryRepository memoryRepository;
    private final CandidateMemoryExtractor extractor;
    private final UnifiedTraceRecorder traceRecorder;

    public CandidateMemoryApplicationService(CandidateRepository candidateRepository,
                                             CandidateMemoryRepository memoryRepository,
                                             CandidateMemoryExtractor extractor,
                                             UnifiedTraceRecorder traceRecorder) {
        this.candidateRepository = candidateRepository;
        this.memoryRepository = memoryRepository;
        this.extractor = extractor;
        this.traceRecorder = traceRecorder;
    }

    public CandidateMemoryFact create(CreateCandidateMemoryCommand command) {
        requireCandidate(command.candidateId());
        CandidateMemoryFact fact = new CandidateMemoryFact(memoryRepository.nextId(), command.candidateId(),
                command.type(), command.factKey(), command.factValue(),
                command.confidence() == null ? 1.0 : command.confidence(),
                command.sourceType() == null ? CandidateMemorySourceType.MANUAL : command.sourceType(),
                command.sourceId(), command.evidence());
        return upsert(fact);
    }

    public List<CandidateMemoryFact> listActive(Long candidateId) {
        requireCandidate(candidateId);
        return memoryRepository.findActiveByCandidateId(candidateId).stream()
                .sorted(Comparator.comparing(CandidateMemoryFact::updatedAt).reversed())
                .toList();
    }

    public List<CandidateMemoryFact> listAll(Long candidateId) {
        requireCandidate(candidateId);
        return memoryRepository.findAllByCandidateId(candidateId).stream()
                .sorted(Comparator.comparing(CandidateMemoryFact::updatedAt).reversed())
                .toList();
    }

    public CandidateMemoryFact deactivate(Long candidateId, Long memoryId) {
        requireCandidate(candidateId);
        CandidateMemoryFact fact = memoryRepository.findById(memoryId)
                .filter(item -> Objects.equals(item.candidateId(), candidateId))
                .orElseThrow(() -> new NoSuchElementException("candidate memory does not exist: " + memoryId));
        return memoryRepository.save(fact.deactivate());
    }

    public List<CandidateMemoryFact> observeMessage(Conversation conversation, ConversationMessage message) {
        if (conversation == null || message == null || message.senderType() != MessageSenderType.CANDIDATE) {
            return List.of();
        }
        try (UnifiedTraceRecorder.Span span = traceRecorder.start(TraceComponent.MEMORY, "extract_candidate_memory",
                java.util.Map.of("candidateId", conversation.candidateId(), "messageId", message.id()))) {
            List<CandidateMemoryFact> extracted = extractor.extract(conversation, message).stream()
                    .map(this::upsert)
                    .toList();
            span.succeed(java.util.Map.of("extracted", extracted.size()));
            return extracted;
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    public String promptContext(Long candidateId) {
        List<CandidateMemoryFact> facts = memoryRepository.findActiveByCandidateId(candidateId);
        if (facts.isEmpty()) {
            return "";
        }
        return facts.stream()
                .sorted(Comparator.comparing(CandidateMemoryFact::confidence).reversed()
                        .thenComparing(CandidateMemoryFact::updatedAt, Comparator.reverseOrder()))
                .limit(12)
                .map(fact -> "- " + fact.type().name().toLowerCase() + "." + fact.factKey() + ": "
                        + fact.factValue() + " (confidence " + Math.round(fact.confidence() * 100) + "%)")
                .collect(Collectors.joining("\n"));
    }

    private CandidateMemoryFact upsert(CandidateMemoryFact fact) {
        return memoryRepository.findActiveByCandidateIdAndKey(fact.candidateId(), fact.factKey())
                .map(existing -> memoryRepository.save(existing.mergeFrom(fact)))
                .orElseGet(() -> memoryRepository.save(fact.id() < 0 ? fact.withId(memoryRepository.nextId()) : fact));
    }

    private void requireCandidate(Long candidateId) {
        candidateRepository.findById(candidateId)
                .orElseThrow(() -> new NoSuchElementException("candidate does not exist: " + candidateId));
    }
}
