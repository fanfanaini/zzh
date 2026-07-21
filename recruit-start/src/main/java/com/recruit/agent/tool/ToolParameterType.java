package com.recruit.agent.tool;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public enum ToolParameterType {
    STRING {
        @Override
        boolean accepts(Object value) {
            return value instanceof String;
        }
    },
    INTEGER {
        @Override
        boolean accepts(Object value) {
            return value instanceof Byte || value instanceof Short || value instanceof Integer;
        }
    },
    LONG {
        @Override
        boolean accepts(Object value) {
            return value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long;
        }
    },
    DECIMAL {
        @Override
        boolean accepts(Object value) {
            return value instanceof Number || value instanceof BigDecimal;
        }
    },
    BOOLEAN {
        @Override
        boolean accepts(Object value) {
            return value instanceof Boolean;
        }
    },
    OBJECT {
        @Override
        boolean accepts(Object value) {
            return value instanceof Map<?, ?>;
        }
    },
    ARRAY {
        @Override
        boolean accepts(Object value) {
            return value instanceof Collection<?> || value.getClass().isArray();
        }
    };

    abstract boolean accepts(Object value);
}
