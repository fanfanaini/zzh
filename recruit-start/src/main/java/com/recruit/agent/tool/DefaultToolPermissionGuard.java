package com.recruit.agent.tool;

import java.util.HashSet;
import java.util.Set;

public final class DefaultToolPermissionGuard implements ToolPermissionGuard {
    @Override
    public Set<String> missingPermissions(ToolDescriptor descriptor, ToolInvocationContext context) {
        Set<String> missing = new HashSet<>(descriptor.requiredPermissions());
        missing.removeAll(context.permissions());
        return Set.copyOf(missing);
    }
}
