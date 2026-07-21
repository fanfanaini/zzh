package com.recruit.agent.auth.application;

public record LoginCommand(
        String username,
        String password
) {
}
