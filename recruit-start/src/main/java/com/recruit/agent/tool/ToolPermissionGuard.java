package com.recruit.agent.tool;

import java.util.Set;

@FunctionalInterface
public interface ToolPermissionGuard {
    Set<String> missingPermissions(ToolDescriptor descriptor, ToolInvocationContext context);
}
