package com.recruit.agent.trace;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class UnifiedTraceRecorder {
    private final AgentTraceSink sink;

    public UnifiedTraceRecorder(AgentTraceSink sink) {
        if (sink == null) {
            throw new IllegalArgumentException("sink must not be null");
        }
        this.sink = sink;
    }

    public Span start(TraceComponent component, String operation, Map<String, Object> details) {
        TraceContext context = TraceContextHolder.current().orElse(null);
        if (context == null) {
            return Span.noop();
        }
        String spanId = UUID.randomUUID().toString();
        record(context, component, operation, spanId, AgentTraceStatus.STARTED, details);
        return new Span(this, context, component, operation, spanId, System.nanoTime(), false);
    }

    public void recordExternal(TraceContext context,
                               TraceComponent component,
                               String operation,
                               String spanId,
                               AgentTraceStatus status,
                               Map<String, Object> details) {
        record(context, component, operation, spanId, status, details);
    }

    private void record(TraceContext context,
                        TraceComponent component,
                        String operation,
                        String spanId,
                        AgentTraceStatus status,
                        Map<String, Object> details) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("component", component.name());
        normalized.put("operation", operation);
        normalized.put("spanId", spanId);
        if (context.parentSpanId() != null) {
            normalized.put("parentSpanId", context.parentSpanId());
        }
        if (details != null) {
            normalized.putAll(details);
        }
        sink.record(new AgentTraceEvent(
                context.traceId(),
                context.conversationId(),
                component.name().toLowerCase() + "." + operation,
                status,
                normalized,
                Instant.now()
        ));
    }

    public static final class Span implements AutoCloseable {
        private final UnifiedTraceRecorder recorder;
        private final TraceContext context;
        private final TraceComponent component;
        private final String operation;
        private final String spanId;
        private final long startedNanos;
        private final boolean noop;
        private boolean completed;

        private Span(UnifiedTraceRecorder recorder,
                     TraceContext context,
                     TraceComponent component,
                     String operation,
                     String spanId,
                     long startedNanos,
                     boolean noop) {
            this.recorder = recorder;
            this.context = context;
            this.component = component;
            this.operation = operation;
            this.spanId = spanId;
            this.startedNanos = startedNanos;
            this.noop = noop;
        }

        private static Span noop() {
            return new Span(null, null, null, null, null, 0, true);
        }

        public String spanId() {
            return spanId;
        }

        public void succeed(Map<String, Object> details) {
            complete(AgentTraceStatus.SUCCEEDED, details);
        }

        public void fail(String errorCode, String message, Map<String, Object> details) {
            Map<String, Object> failure = new LinkedHashMap<>();
            if (details != null) {
                failure.putAll(details);
            }
            failure.put("errorCode", errorCode);
            failure.put("error", message);
            complete(AgentTraceStatus.FAILED, failure);
        }

        private void complete(AgentTraceStatus status, Map<String, Object> details) {
            if (noop || completed) {
                return;
            }
            completed = true;
            Map<String, Object> terminal = new LinkedHashMap<>();
            if (details != null) {
                terminal.putAll(details);
            }
            terminal.put("durationMillis", Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000));
            recorder.record(context, component, operation, spanId, status, terminal);
        }

        @Override
        public void close() {
            if (!noop && !completed) {
                fail("UNFINISHED", "Trace span closed without a terminal result", Map.of());
            }
        }
    }
}
