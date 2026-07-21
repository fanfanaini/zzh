package com.recruit.agent.workflow;

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
import com.recruit.agent.conversation.domain.ReplyIntent;
import com.recruit.agent.conversation.domain.ReplySuggestionDraft;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.tool.RecruitmentToolConfiguration;
import com.recruit.agent.tool.ToolInvocationContext;
import com.recruit.agent.tool.ToolResult;
import com.recruit.agent.tool.ToolRuntime;
import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceSink;
import com.recruit.agent.trace.AgentTraceStatus;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public final class LangGraphReplySuggestionGenerator implements ReplySuggestionGenerator {
    private static final String MEMORY_NODE = "memory";
    private static final String CLASSIFY_NODE = "classify";
    private static final String ROUTE_NODE = "route";
    private static final String TOOL_NODE = "tool_context";
    private static final String GENERATE_NODE = "generate_reply";
    private static final String SAFE_NODE = "safe_reply";
    private static final String HUMAN_NODE = "human_draft";
    private static final String SCHEDULING_NODE = "scheduling_handoff";

    private final ReplySuggestionGenerator delegate;
    private final ConversationMemoryAssembler memoryAssembler;
    private final ConversationIntentClassifier intentClassifier;
    private final ConversationRoutingPolicy routingPolicy;
    private final ToolRuntime toolRuntime;
    private final AgentTraceSink traceSink;
    private final CompiledGraph<RecruitmentConversationAgentState> graph;

    public LangGraphReplySuggestionGenerator(ReplySuggestionGenerator delegate,
                                             ConversationMemoryAssembler memoryAssembler,
                                             ConversationIntentClassifier intentClassifier,
                                             ConversationRoutingPolicy routingPolicy,
                                             ToolRuntime toolRuntime,
                                             AgentTraceSink traceSink) {
        this.delegate = delegate;
        this.memoryAssembler = memoryAssembler;
        this.intentClassifier = intentClassifier;
        this.routingPolicy = routingPolicy;
        this.toolRuntime = toolRuntime;
        this.traceSink = traceSink;
        this.graph = buildGraph();
    }

    @Override
    public ReplySuggestionDraft generate(Job job, Candidate candidate, Conversation conversation,
                                         List<ConversationMessage> messages) {
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> initial = Map.of(
                RecruitmentConversationAgentState.TRACE_ID, traceId,
                RecruitmentConversationAgentState.JOB, job,
                RecruitmentConversationAgentState.CANDIDATE, candidate,
                RecruitmentConversationAgentState.CONVERSATION, conversation,
                RecruitmentConversationAgentState.MESSAGES, messages == null ? List.of() : List.copyOf(messages)
        );
        try {
            return graph.invoke(initial).orElseThrow(() -> new IllegalStateException("Agent graph returned no state"))
                    .suggestion();
        } catch (RuntimeException exception) {
            traceSink.record(new AgentTraceEvent(traceId, conversation.id(), "run", AgentTraceStatus.FAILED,
                    Map.of("error", safeMessage(exception)), Instant.now()));
            ReplySuggestionDraft fallback = delegate.generate(job, candidate, conversation,
                    messages == null ? List.of() : messages);
            return new ReplySuggestionDraft(fallback.intent(), fallback.confidence(), true, fallback.suggestedReply());
        }
    }

    private CompiledGraph<RecruitmentConversationAgentState> buildGraph() {
        try {
            return new StateGraph<>(RecruitmentConversationAgentState::new)
                    .addNode(MEMORY_NODE, node_async(this::assembleMemory))
                    .addNode(CLASSIFY_NODE, node_async(this::classify))
                    .addNode(ROUTE_NODE, node_async(this::selectRoute))
                    .addNode(TOOL_NODE, node_async(this::invokeTool))
                    .addNode(GENERATE_NODE, node_async(this::generateReply))
                    .addNode(SAFE_NODE, node_async(this::safeReply))
                    .addNode(HUMAN_NODE, node_async(this::humanDraft))
                    .addNode(SCHEDULING_NODE, node_async(this::schedulingDraft))
                    .addEdge(START, MEMORY_NODE)
                    .addEdge(MEMORY_NODE, CLASSIFY_NODE)
                    .addEdge(CLASSIFY_NODE, ROUTE_NODE)
                    .addConditionalEdges(ROUTE_NODE, edge_async(state -> state.route().name()), Map.of(
                            ConversationRoute.FAQ_RAG.name(), TOOL_NODE,
                            ConversationRoute.JOB_INFO.name(), TOOL_NODE,
                            ConversationRoute.CANDIDATE_STATUS.name(), TOOL_NODE,
                            ConversationRoute.INTERVIEW_SCHEDULING.name(), SCHEDULING_NODE,
                            ConversationRoute.SAFE_REPLY.name(), SAFE_NODE,
                            ConversationRoute.HUMAN_HANDOFF.name(), HUMAN_NODE,
                            ConversationRoute.GENERAL_REPLY.name(), GENERATE_NODE
                    ))
                    .addEdge(TOOL_NODE, GENERATE_NODE)
                    .addEdge(GENERATE_NODE, END)
                    .addEdge(SAFE_NODE, END)
                    .addEdge(HUMAN_NODE, END)
                    .addEdge(SCHEDULING_NODE, END)
                    .compile();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to compile recruitment conversation graph", exception);
        }
    }

    private Map<String, Object> assembleMemory(RecruitmentConversationAgentState state) {
        ConversationMemory memory = memoryAssembler.assemble(
                state.job(), state.candidate(), state.conversation(), state.messages());
        trace(state, MEMORY_NODE, Map.of("recentMessages", memory.recentMessages().size(),
                "truncated", memory.truncated(), "characters", memory.characterCount()));
        return Map.of(RecruitmentConversationAgentState.MEMORY, memory);
    }

    private Map<String, Object> classify(RecruitmentConversationAgentState state) {
        IntentClassificationResult classification = intentClassifier.classify(new IntentClassificationRequest(
                state.job(), state.candidate(), state.conversation(), state.memory()));
        trace(state, CLASSIFY_NODE, Map.of("intent", classification.intent().name(),
                "confidence", classification.confidence(), "handoff", classification.handoffRequired(),
                "risks", classification.risks().stream().map(Enum::name).toList()));
        return Map.of(RecruitmentConversationAgentState.CLASSIFICATION, classification);
    }

    private Map<String, Object> selectRoute(RecruitmentConversationAgentState state) {
        ConversationRoute route = routingPolicy.route(state.classification());
        trace(state, ROUTE_NODE, Map.of("route", route.name(), "reason", state.classification().reason()));
        return Map.of(RecruitmentConversationAgentState.ROUTE, route);
    }

    private Map<String, Object> invokeTool(RecruitmentConversationAgentState state) {
        String toolName;
        Map<String, Object> arguments;
        switch (state.route()) {
            case FAQ_RAG -> {
                toolName = "search_knowledge";
                arguments = Map.of("query", latestCandidateMessage(state.memory()), "limit", 3);
            }
            case JOB_INFO -> {
                toolName = "get_job_info";
                arguments = Map.of("jobId", state.job().id());
            }
            case CANDIDATE_STATUS -> {
                toolName = "get_candidate_status";
                arguments = Map.of("candidateId", state.candidate().id());
            }
            default -> throw new IllegalStateException("Route does not map to a tool: " + state.route());
        }
        ToolInvocationContext context = new ToolInvocationContext(state.traceId(), "recruitment-agent",
                Set.of(RecruitmentToolConfiguration.READ_PERMISSION), null,
                Map.of("conversationId", state.conversation().id()));
        ToolResult result = toolRuntime.invoke(toolName, arguments, context);
        trace(state, TOOL_NODE, Map.of("tool", toolName, "success", result.success(),
                "attempts", result.attempts(), "toolTraceId", result.traceId()));
        String contextText = result.success()
                ? "Tool " + toolName + " returned: " + result.data()
                : "Tool " + toolName + " failed: " + result.error().code() + " " + result.error().message();
        return Map.of(RecruitmentConversationAgentState.TOOL_CONTEXT, contextText);
    }

    private Map<String, Object> generateReply(RecruitmentConversationAgentState state) {
        ReplySuggestionDraft generated = delegate.generate(state.job(), state.candidate(), state.conversation(),
                messagesForGeneration(state));
        IntentClassificationResult classification = state.classification();
        ReplySuggestionDraft result = new ReplySuggestionDraft(
                classification.intent() == ReplyIntent.UNKNOWN ? generated.intent() : classification.intent(),
                classification.intent() == ReplyIntent.UNKNOWN ? generated.confidence() : classification.confidence(),
                generated.needHuman(), generated.suggestedReply());
        trace(state, GENERATE_NODE, Map.of("needHuman", result.needHuman(), "intent", result.intent().name()));
        return Map.of(RecruitmentConversationAgentState.SUGGESTION, result);
    }

    private Map<String, Object> safeReply(RecruitmentConversationAgentState state) {
        ReplySuggestionDraft result = new ReplySuggestionDraft(ReplyIntent.OUT_OF_DOMAIN,
                state.classification().confidence(), false,
                "我只能协助处理岗位、招聘流程和面试相关问题。你可以继续询问当前岗位的信息。");
        trace(state, SAFE_NODE, Map.of("intent", ReplyIntent.OUT_OF_DOMAIN.name()));
        return Map.of(RecruitmentConversationAgentState.SUGGESTION, result);
    }

    private Map<String, Object> humanDraft(RecruitmentConversationAgentState state) {
        ReplySuggestionDraft generated = delegate.generate(state.job(), state.candidate(), state.conversation(),
                messagesForGeneration(state));
        ReplySuggestionDraft result = new ReplySuggestionDraft(state.classification().intent(),
                state.classification().confidence(), true, generated.suggestedReply());
        trace(state, HUMAN_NODE, Map.of("risks", state.classification().risks().stream().map(Enum::name).toList()));
        return Map.of(RecruitmentConversationAgentState.SUGGESTION, result);
    }

    private Map<String, Object> schedulingDraft(RecruitmentConversationAgentState state) {
        ReplySuggestionDraft result = new ReplySuggestionDraft(ReplyIntent.ASK_AVAILABILITY,
                state.classification().confidence(), true,
                "已记录你的面试时间诉求，需要 HR 确认可用时间后再完成预约。");
        trace(state, SCHEDULING_NODE, Map.of("reason", "No interview calendar tool is configured"));
        return Map.of(RecruitmentConversationAgentState.SUGGESTION, result);
    }

    private List<ConversationMessage> messagesForGeneration(RecruitmentConversationAgentState state) {
        List<ConversationMessage> messages = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        if (!state.memory().summary().isBlank()) context.append("Summary: ").append(state.memory().summary()).append('\n');
        context.append("Facts: ").append(state.memory().facts()).append('\n');
        context.append("State: ").append(state.memory().state());
        if (!state.toolContext().isBlank()) context.append('\n').append(state.toolContext());
        messages.add(new ConversationMessage(Long.MIN_VALUE, state.conversation().id(), MessageSenderType.SYSTEM,
                context.toString(), Instant.EPOCH));
        messages.addAll(state.memory().recentMessages());
        return List.copyOf(messages);
    }

    private String latestCandidateMessage(ConversationMemory memory) {
        return memory.recentMessages().stream()
                .filter(message -> message.senderType() == MessageSenderType.CANDIDATE)
                .reduce((first, second) -> second)
                .map(ConversationMessage::content)
                .orElse("");
    }

    private void trace(RecruitmentConversationAgentState state, String node, Map<String, Object> details) {
        traceSink.record(new AgentTraceEvent(state.traceId(), state.conversation().id(), node,
                AgentTraceStatus.SUCCEEDED, details, Instant.now()));
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
