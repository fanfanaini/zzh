package com.recruit.agent.conversation.api;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ScheduleFollowUpRequest(
        @NotNull
        Instant nextFollowUpAt
) {
}
