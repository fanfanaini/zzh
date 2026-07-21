package com.recruit.agent.trace;

@FunctionalInterface
public interface ToolTraceSink {
    void record(ToolTrace trace);
}
