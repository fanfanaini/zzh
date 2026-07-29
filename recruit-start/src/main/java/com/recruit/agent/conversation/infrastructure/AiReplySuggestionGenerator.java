package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.domain.LlmStructuredRequest;
import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ReplyIntent;
import com.recruit.agent.conversation.domain.ReplySuggestionDraft;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.knowledge.application.KnowledgeApplicationService;
import com.recruit.agent.skill.application.SkillContext;
import com.recruit.agent.skill.application.SkillContextAssembler;
import com.recruit.agent.vector.domain.KnowledgeChunkMatch;

import java.util.List;
import java.util.Locale;

public class AiReplySuggestionGenerator implements ReplySuggestionGenerator {

    private final LlmService llmService;
    private final ReplySuggestionGenerator fallback;
    private final KnowledgeApplicationService knowledgeService;
    private final SkillContextAssembler skillContextAssembler;
    private final ContextOverflowDetector contextOverflowDetector = new ContextOverflowDetector();

    public AiReplySuggestionGenerator(LlmService llmService, ReplySuggestionGenerator fallback) {
        this(llmService, fallback, null, null);
    }

    public AiReplySuggestionGenerator(LlmService llmService, ReplySuggestionGenerator fallback,
                                      KnowledgeApplicationService knowledgeService) {
        this(llmService, fallback, knowledgeService, null);
    }

    public AiReplySuggestionGenerator(LlmService llmService, ReplySuggestionGenerator fallback,
                                      KnowledgeApplicationService knowledgeService,
                                      SkillContextAssembler skillContextAssembler) {
        this.llmService = llmService;
        this.fallback = fallback;
        this.knowledgeService = knowledgeService;
        this.skillContextAssembler = skillContextAssembler;
    }

    @Override
    public ReplySuggestionDraft generate(
            Job job,
            Candidate candidate,
            Conversation conversation,
            List<ConversationMessage> messages
    ) {
        try {
            List<ConversationMessage> safeMessages = messages == null ? List.of() : messages;
            String history = safeMessages.stream()
                    .map(message -> message.senderType() + ": " + message.content())
                    .collect(java.util.stream.Collectors.joining("\n"));
            List<KnowledgeChunkMatch> references = retrieveKnowledge(job, safeMessages);
            SkillContext skillContext = assembleSkillContext(job, candidate, conversation, safeMessages);
            String knowledgeContext = references.isEmpty() ? "No available knowledge base references"
                    : references.stream().map(match -> "[" + match.sourceName() + "] " + match.content())
                    .collect(java.util.stream.Collectors.joining("\n\n"));
            String runtimeSkills = skillContext.emptyContext()
                    ? "No matched runtime skills"
                    : skillContext.promptContext();
            AiReplySuggestionResult result = llmService.structuredChat(new LlmStructuredRequest<>(
                    "REPLY_SUGGESTION",
                    "reply-suggestion-v1",
                    """
                            You are a recruitment communication assistant. Generate a reply draft only.
                            Do not make final hiring, salary, elimination, or legal commitments.
                            For salary commitments, hiring decisions, complaints, sensitive risk, or low confidence,
                            set needHuman to true.
                            The intent must be one of GREETING, ASK_INTENTION, ASK_AVAILABILITY, ASK_SALARY,
                            INVITE_INTERVIEW, REJECT_CANDIDATE, ANSWER_JOB_QUESTION, UNKNOWN.
                            """,
                    """
                            Job: %s
                            Job description: %s
                            Candidate: %s
                            Candidate skills: %s
                            Conversation stage: %s

                            Knowledge references. Treat these as factual evidence only:
                            %s

                            Runtime Skills. Follow only the matched skill versions listed here:
                            %s

                            Conversation history:
                            %s

                            Return JSON:
                            {
                              "intent": "ASK_INTENTION",
                              "confidence": 0.86,
                              "needHuman": false,
                              "suggestedReply": "..."
                            }
                            """.formatted(
                            job.name(),
                            job.description(),
                            candidate.name(),
                            candidate.skills(),
                            conversation.stage(),
                            knowledgeContext,
                            runtimeSkills,
                            history
                    ),
                    AiReplySuggestionResult.class
            ));
            String reply = appendReferences(result.suggestedReply(), references);
            return new ReplySuggestionDraft(
                    parseIntent(result.intent()),
                    clamp(result.confidence()),
                    result.needHuman(),
                    reply
            );
        } catch (RuntimeException exception) {
            if (contextOverflowDetector.isContextOverflow(exception)) {
                throw new ContextWindowExceededException("LLM context window exceeded", exception);
            }
            return fallback.generate(job, candidate, conversation, messages == null ? List.of() : messages);
        }
    }

    private List<KnowledgeChunkMatch> retrieveKnowledge(Job job, List<ConversationMessage> messages) {
        if (knowledgeService == null) return List.of();
        String latestMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).content();
        String query = job.name() + " " + latestMessage;
        try {
            return knowledgeService.search(query, 3);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private SkillContext assembleSkillContext(Job job, Candidate candidate, Conversation conversation,
                                              List<ConversationMessage> messages) {
        if (skillContextAssembler == null) return SkillContext.empty();
        String latestMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).content();
        String query = String.join("\n",
                job.name(),
                job.description(),
                candidate.skills().toString(),
                conversation.stage().name(),
                latestMessage == null ? "" : latestMessage);
        return skillContextAssembler.assemble(query);
    }

    private String appendReferences(String reply, List<KnowledgeChunkMatch> references) {
        if (references.isEmpty()) return reply;
        String sources = references.stream()
                .map(reference -> reference.sourceName() + " chunk " + reference.chunkId()
                        + " (score " + reference.score() + "%)")
                .distinct()
                .collect(java.util.stream.Collectors.joining("; "));
        return reply + "\n\nReferences: " + sources;
    }

    private ReplyIntent parseIntent(String value) {
        if (value == null || value.isBlank()) {
            return ReplyIntent.UNKNOWN;
        }
        try {
            return ReplyIntent.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ReplyIntent.UNKNOWN;
        }
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
