package com.recruit.agent.mcp.api;

import com.recruit.agent.tool.*;
import com.recruit.agent.trace.InMemoryToolTraceSink;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpToolControllerTest {

    @Test
    void listsToolsAsMcpStyleSchemasAndCallsTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ToolDescriptor("get_job_info", "Read job",
                new ToolParameterContract(List.of(new ToolParameter("jobId", ToolParameterType.LONG, true, "job id")),
                        true),
                Set.of("recruit.read"), Duration.ofSeconds(1), 1, false),
                (context, arguments) -> Map.of("id", ((Number) arguments.get("jobId")).longValue()));
        ToolRuntime runtime = new ToolRuntime(registry, new DefaultToolPermissionGuard(),
                new InMemoryToolIdempotencyStore(), new InMemoryToolTraceSink());
        McpToolController controller = new McpToolController(registry, runtime);

        List<McpToolResponse> tools = controller.listTools();
        McpToolCallResponse response = controller.call("get_job_info",
                new McpToolCallRequest(Map.of("jobId", 123L), null, 1L));

        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).inputSchema()).containsEntry("type", "object");
        assertThat(response.success()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.data();
        assertThat(data).containsEntry("id", 123L);
    }
}
