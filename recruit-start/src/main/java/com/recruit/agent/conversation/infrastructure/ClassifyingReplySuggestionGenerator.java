package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationIntentClassifier;
import com.recruit.agent.conversation.domain.ConversationMemory;
import com.recruit.agent.conversation.domain.ConversationMemoryAssembler;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationRoute;
import com.recruit.agent.conversation.domain.ConversationRoutingPolicy;
import com.recruit.agent.conversation.domain.IntentClassificationRequest;
import com.recruit.agent.conversation.domain.IntentClassificationResult;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.conversation.domain.ReplySuggestionDraft;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.conversation.domain.ReplyIntent;
import com.recruit.agent.job.domain.Job;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassifyingReplySuggestionGenerator implements ReplySuggestionGenerator {

    private final ReplySuggestionGenerator delegate;
    private final ConversationMemoryAssembler memoryAssembler;
    private final ConversationIntentClassifier intentClassifier;
    private final ConversationRoutingPolicy routingPolicy;

    public ClassifyingReplySuggestionGenerator(ReplySuggestionGenerator delegate,
                                               ConversationMemoryAssembler memoryAssembler,
                                               ConversationIntentClassifier intentClassifier,
                                               ConversationRoutingPolicy routingPolicy) {
        this.delegate = delegate;
        this.memoryAssembler = memoryAssembler;
        this.intentClassifier = intentClassifier;
        this.routingPolicy = routingPolicy;
    }

    @Override
    public ReplySuggestionDraft generate(Job job, Candidate candidate, Conversation conversation,
                                         List<ConversationMessage> messages) {
        ConversationMemory memory = memoryAssembler.assemble(job, candidate, conversation, messages);
        IntentClassificationResult classification = intentClassifier.classify(
                new IntentClassificationRequest(job, candidate, conversation, memory));
        ReplySuggestionDraft generated = delegate.generate(job, candidate, conversation,
                messagesForGeneration(conversation, memory));
        IntentClassificationResult effectiveClassification = effectiveClassification(classification, generated);
        ConversationRoute route = routingPolicy.route(effectiveClassification);
        return new ReplySuggestionDraft(
                effectiveClassification.intent(),
                effectiveClassification.confidence(),
                generated.needHuman() || route == ConversationRoute.HUMAN_HANDOFF,
                generated.suggestedReply()
        );
    }

    private IntentClassificationResult effectiveClassification(IntentClassificationResult classified,
                                                                ReplySuggestionDraft generated) {
        if (classified.intent() != ReplyIntent.UNKNOWN || classified.handoffRequired()
                || !classified.risks().isEmpty()) {
            return classified;
        }
        return new IntentClassificationResult(
                generated.intent(),
                generated.confidence(),
                java.util.Set.of(),
                generated.needHuman(),
                "Reply generator classification used after no deterministic rule matched"
        );
    }

    private List<ConversationMessage> messagesForGeneration(Conversation conversation, ConversationMemory memory) {
        List<ConversationMessage> messages = new ArrayList<>();
        String context = renderContext(memory);
        if (!context.isBlank()) {
            messages.add(new ConversationMessage(Long.MIN_VALUE, conversation.id(), MessageSenderType.SYSTEM,
                    context, Instant.EPOCH));
        }
        messages.addAll(memory.recentMessages());
        return List.copyOf(messages);
    }

    private String renderContext(ConversationMemory memory) {
        String facts = renderMap(memory.facts());
        String state = renderMap(memory.state());
        String summary = memory.summary().isBlank() ? "" : "Summary: " + memory.summary();
        return java.util.stream.Stream.of("Facts: " + facts, "State: " + state, summary)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String renderMap(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
