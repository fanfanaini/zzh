package com.recruit.agent.tool;

public record ToolParameter(
        String name,
        ToolParameterType type,
        boolean required,
        String description
) {
    public ToolParameter {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool parameter name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Tool parameter type must not be null");
        }
        name = name.trim();
        description = description == null ? "" : description.trim();
    }
}
