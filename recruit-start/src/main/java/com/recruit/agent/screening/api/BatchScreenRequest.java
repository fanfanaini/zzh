package com.recruit.agent.screening.api;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchScreenRequest(@NotEmpty List<Long> candidateIds) {
}
