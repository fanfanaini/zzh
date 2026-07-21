package com.recruit.agent.tool;

import java.util.Map;

@FunctionalInterface
public interface ToolHandler {
    Object execute(ToolInvocationContext context, Map<String, Object> arguments) throws Exception;
}
