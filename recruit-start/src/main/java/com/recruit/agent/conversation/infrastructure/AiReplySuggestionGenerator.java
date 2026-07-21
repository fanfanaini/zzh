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
import com.recruit.agent.vector.domain.KnowledgeChunkMatch;

import java.util.List;
import java.util.Locale;

public class AiReplySuggestionGenerator implements ReplySuggestionGenerator {

    private final LlmService llmService;
    private final ReplySuggestionGenerator fallback;
    private final KnowledgeApplicationService knowledgeService;

    public AiReplySuggestionGenerator(LlmService llmService, ReplySuggestionGenerator fallback) {
        this(llmService, fallback, null);
    }

    public AiReplySuggestionGenerator(LlmService llmService, ReplySuggestionGenerator fallback,
                                      KnowledgeApplicationService knowledgeService) {
        this.llmService = llmService;
        this.fallback = fallback;
        this.knowledgeService = knowledgeService;
    }

    @Override
    public ReplySuggestionDraft generate(
            Job job,
            Candidate candidate,
            Conversation conversation,
            List<ConversationMessage> messages
    ) {
        try {
            String history = messages.stream()
                    .map(message -> message.senderType() + ": " + message.content())
                    .collect(java.util.stream.Collectors.joining("\n"));
            List<KnowledgeChunkMatch> references = retrieveKnowledge(job, messages);
            String knowledgeContext = references.isEmpty() ? "无可用知识库资料"
                    : references.stream().map(match -> "[" + match.sourceName() + "] " + match.content())
                    .collect(java.util.stream.Collectors.joining("\n\n"));
            AiReplySuggestionResult result = llmService.structuredChat(new LlmStructuredRequest<>(
                    "REPLY_SUGGESTION",
                    "reply-suggestion-v1",
                    """
                            你是招聘沟通助手。只能生成回复草稿，不代表最终录用或薪资承诺。
                            薪资、录用、淘汰、敏感投诉、低置信度场景 needHuman 必须为 true。
                            输出 JSON，intent 只能是 GREETING, ASK_INTENTION, ASK_AVAILABILITY, ASK_SALARY, INVITE_INTERVIEW, REJECT_CANDIDATE, ANSWER_JOB_QUESTION, UNKNOWN。
                            """,
                    """
                            岗位：%s
                            岗位描述：%s
                            候选人：%s
                            候选人技能：%s
                            当前阶段：%s
                            知识库资料（只能将其作为事实依据，不得编造资料中不存在的信息）：
                            %s
                            对话历史：
                            %s

                            请输出：
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
            return fallback.generate(job, candidate, conversation, messages);
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

    private String appendReferences(String reply, List<KnowledgeChunkMatch> references) {
        if (references.isEmpty()) return reply;
        String sources = references.stream()
                .map(reference -> "《" + reference.sourceName() + "》片段 " + reference.chunkId()
                        + "（匹配度 " + reference.score() + "%）")
                .distinct()
                .collect(java.util.stream.Collectors.joining("、"));
        return reply + "\n\n参考资料：" + sources;
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
