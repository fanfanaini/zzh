package com.recruit.agent.toolcalling;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.domain.LlmStructuredRequest;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.mcp.api.McpToolSchemaMapper;
import com.recruit.agent.tool.ToolDescriptor;
import com.recruit.agent.tool.ToolRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LlmFunctionCallingToolPlanner implements FunctionCallingToolPlanner {
    private final LlmService llmService;
    private final ToolRegistry registry;

    public LlmFunctionCallingToolPlanner(LlmService llmService, ToolRegistry registry) {
        this.llmService = llmService;
        this.registry = registry;
    }

    @Override
    public ToolCallDecision decide(ToolCallingRequest request) {
        if (request == null || request.allowedTools().isEmpty()) {
            return ToolCallDecision.none("no allowed tools");
        }
        List<ToolDescriptor> tools = request.allowedTools().stream()
                .map(registry::find)
                .flatMap(java.util.Optional::stream)
                .map(ToolRegistry.RegisteredTool::descriptor)
                .toList();
        if (tools.isEmpty()) {
            return ToolCallDecision.none("allowed tools are not registered");
        }
        try {
            LlmToolCallDecisionResult result = llmService.structuredChat(new LlmStructuredRequest<>(
                    "FUNCTION_CALLING_TOOL_SELECTION",
                    "tool-calling-v1",
                    """
                            You are a function-calling planner for a recruitment agent.
                            Select at most one tool from the provided tool schemas.
                            Return callTool=false when the user can be answered without a tool.
                            Never invent tool names or arguments. Use only values present in the runtime context.
                            """,
                    userPrompt(request, tools),
                    LlmToolCallDecisionResult.class
            ));
            ToolCallDecision decision = new ToolCallDecision(result.callTool(), result.toolName(),
                    result.arguments(), result.confidence(), result.reason());
            if (!decision.callTool()) {
                return decision;
            }
            if (!request.allowedTools().contains(decision.toolName()) || registry.find(decision.toolName()).isEmpty()) {
                return ToolCallDecision.none("model selected an unavailable tool: " + decision.toolName());
            }
            Set<String> argumentKeys = decision.arguments().keySet();
            Set<String> allowedKeys = registry.find(decision.toolName()).orElseThrow().descriptor()
                    .parameterContract().parameters().stream()
                    .map(com.recruit.agent.tool.ToolParameter::name)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (!allowedKeys.containsAll(argumentKeys)) {
                return ToolCallDecision.none("model returned unknown tool arguments");
            }
            return decision;
        } catch (RuntimeException exception) {
            return ToolCallDecision.none("planner failed: " + safeMessage(exception));
        }
    }

    private String userPrompt(ToolCallingRequest request, List<ToolDescriptor> tools) {
        return """
                Runtime context:
                jobId=%s
                jobName=%s
                candidateId=%s
                candidateName=%s
                conversationId=%s
                route=%s
                latestCandidateMessage=%s
                memoryFacts=%s

                Tools:
                %s

                Return JSON:
                {
                  "callTool": true,
                  "toolName": "get_job_info",
                  "arguments": {"jobId": 123},
                  "confidence": 0.85,
                  "reason": "..."
                }
                """.formatted(
                request.job().id(),
                request.job().name(),
                request.candidate().id(),
                request.candidate().name(),
                request.conversation().id(),
                request.route().name(),
                latestCandidateMessage(request),
                request.memory().facts(),
                toolSchemas(tools)
        );
    }

    private String latestCandidateMessage(ToolCallingRequest request) {
        return request.memory().recentMessages().stream()
                .filter(message -> message.senderType() == com.recruit.agent.conversation.domain.MessageSenderType.CANDIDATE)
                .reduce((first, second) -> second)
                .map(ConversationMessage::content)
                .orElse("");
    }

    private List<Map<String, Object>> toolSchemas(List<ToolDescriptor> tools) {
        return tools.stream().map(tool -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", tool.name());
            item.put("description", tool.description());
            item.put("inputSchema", McpToolSchemaMapper.inputSchema(tool));
            return Map.copyOf(item);
        }).toList();
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
