package com.recruit.agent.mcp.api;

import com.recruit.agent.tool.ToolRegistry;
import com.recruit.agent.tool.ToolRuntime;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
public class McpToolController {
    private final ToolRegistry registry;
    private final ToolRuntime runtime;

    public McpToolController(ToolRegistry registry, ToolRuntime runtime) {
        this.registry = registry;
        this.runtime = runtime;
    }

    @GetMapping("/tools")
    public List<McpToolResponse> listTools() {
        return registry.descriptors().stream()
                .sorted(Comparator.comparing(com.recruit.agent.tool.ToolDescriptor::name))
                .map(McpToolResponse::from)
                .toList();
    }

    @PostMapping("/tools/{name}/call")
    public McpToolCallResponse call(@PathVariable String name, @RequestBody(required = false) McpToolCallRequest request) {
        Map<String, Object> arguments = request == null || request.arguments() == null ? Map.of() : request.arguments();
        return McpToolCallResponse.from(runtime.invoke(name, arguments, McpInvocationContextFactory.create(request)));
    }
}
