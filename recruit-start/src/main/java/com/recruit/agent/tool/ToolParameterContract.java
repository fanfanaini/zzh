package com.recruit.agent.tool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ToolParameterContract {
    private final Map<String, ToolParameter> parameters;
    private final boolean rejectUnknownParameters;

    public ToolParameterContract(List<ToolParameter> parameters, boolean rejectUnknownParameters) {
        Map<String, ToolParameter> indexed = new LinkedHashMap<>();
        for (ToolParameter parameter : parameters == null ? List.<ToolParameter>of() : parameters) {
            if (indexed.putIfAbsent(parameter.name(), parameter) != null) {
                throw new IllegalArgumentException("Duplicate tool parameter: " + parameter.name());
            }
        }
        this.parameters = Map.copyOf(indexed);
        this.rejectUnknownParameters = rejectUnknownParameters;
    }

    public static ToolParameterContract none() {
        return new ToolParameterContract(List.of(), true);
    }

    public List<ToolParameter> parameters() {
        return List.copyOf(parameters.values());
    }

    public boolean rejectUnknownParameters() {
        return rejectUnknownParameters;
    }

    public List<String> validate(Map<String, Object> arguments) {
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        List<String> violations = new ArrayList<>();
        for (ToolParameter parameter : parameters.values()) {
            Object value = safeArguments.get(parameter.name());
            if (value == null) {
                if (parameter.required()) {
                    violations.add("Missing required parameter: " + parameter.name());
                }
            } else if (!parameter.type().accepts(value)) {
                violations.add("Parameter '" + parameter.name() + "' must be " + parameter.type());
            }
        }
        if (rejectUnknownParameters) {
            Set<String> unknown = new HashSet<>(safeArguments.keySet());
            unknown.removeAll(parameters.keySet());
            unknown.stream().sorted().forEach(name -> violations.add("Unknown parameter: " + name));
        }
        return List.copyOf(violations);
    }
}
