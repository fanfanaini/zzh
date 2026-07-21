package com.recruit.agent.trace;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryToolTraceSink implements ToolTraceSink {
    private final CopyOnWriteArrayList<ToolTrace> traces = new CopyOnWriteArrayList<>();

    @Override
    public void record(ToolTrace trace) {
        traces.add(trace);
    }

    public List<ToolTrace> findAll() {
        return List.copyOf(traces);
    }

    public void clear() {
        traces.clear();
    }
}
