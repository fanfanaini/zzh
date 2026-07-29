package com.recruit.agent.mcp.api;

import com.recruit.agent.tool.ToolDescriptor;
import com.recruit.agent.tool.ToolParameter;
import com.recruit.agent.tool.ToolParameterType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class McpToolSchemaMapper {
    private McpToolSchemaMapper() {
    }

    public static Map<String, Object> inputSchema(ToolDescriptor descriptor) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = descriptor.parameterContract().parameters().stream()
                .filter(ToolParameter::required)
                .map(ToolParameter::name)
                .toList();
        for (ToolParameter parameter : descriptor.parameterContract().parameters()) {
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("type", jsonSchemaType(parameter.type()));
            if (!parameter.description().isBlank()) {
                property.put("description", parameter.description());
            }
            properties.put(parameter.name(), property);
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", !descriptor.parameterContract().rejectUnknownParameters());
        return Map.copyOf(schema);
    }

    private static String jsonSchemaType(ToolParameterType type) {
        return switch (type) {
            case STRING -> "string";
            case INTEGER, LONG -> "integer";
            case DECIMAL -> "number";
            case BOOLEAN -> "boolean";
            case OBJECT -> "object";
            case ARRAY -> "array";
        };
    }
}
