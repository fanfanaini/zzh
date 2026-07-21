package com.recruit.agent.tool;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ToolRegistry {
    private final ConcurrentMap<String, RegisteredTool> tools = new ConcurrentHashMap<>();

    public void register(ToolDescriptor descriptor, ToolHandler handler) {
        if (descriptor == null || handler == null) {
            throw new IllegalArgumentException("Tool descriptor and handler must not be null");
        }
        RegisteredTool previous = tools.putIfAbsent(descriptor.name(), new RegisteredTool(descriptor, handler));
        if (previous != null) {
            throw new ToolRegistryException(ToolErrorCode.DUPLICATE_TOOL,
                    "Tool is already registered: " + descriptor.name());
        }
    }

    public Optional<RegisteredTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<ToolDescriptor> descriptors() {
        return tools.values().stream().map(RegisteredTool::descriptor).toList();
    }

    public record RegisteredTool(ToolDescriptor descriptor, ToolHandler handler) {
    }
}
