package com.recruit.agent.skill.api;

import jakarta.validation.constraints.NotBlank;

public record SkillMatchRequest(@NotBlank String query, Integer limit) {
}
