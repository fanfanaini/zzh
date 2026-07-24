package com.recruit.agent.trace;

import java.util.Optional;

public final class TraceContextHolder {
    private static final ThreadLocal<TraceContext> CURRENT = new ThreadLocal<>();

    private TraceContextHolder() {
    }

    public static Optional<TraceContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Scope open(TraceContext context) {
        TraceContext previous = CURRENT.get();
        CURRENT.set(context);
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
