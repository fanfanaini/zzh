package com.recruit.agent.mcp.api;

import com.recruit.agent.tool.ToolDescriptor;

import java.util.Map;
import java.util.Set;

public record McpToolResponse(
        String name,
        String description,
        Map<String, Object> inputSchema,
        Set<String> requiredPermissions,
        boolean idempotent
) {
    public static McpToolResponse from(ToolDescriptor descriptor) {
        return new McpToolResponse(descriptor.name(), descriptor.description(),
                McpToolSchemaMapper.inputSchema(descriptor), descriptor.requiredPermissions(), descriptor.idempotent());
    }
}
