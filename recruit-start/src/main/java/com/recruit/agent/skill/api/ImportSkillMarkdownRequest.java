package com.recruit.agent.skill.api;

import jakarta.validation.constraints.NotBlank;

public record ImportSkillMarkdownRequest(@NotBlank String markdown) {
}
