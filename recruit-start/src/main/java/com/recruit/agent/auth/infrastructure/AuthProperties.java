package com.recruit.agent.auth.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recruit.security")
public class AuthProperties {

    private boolean enabled;
    private String jwtSecret = "recruit-agent-local-development-secret-please-change";
    private long jwtExpiresInSeconds = 7200;
    private long refreshTokenExpiresInSeconds = 604800;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getJwtExpiresInSeconds() {
        return jwtExpiresInSeconds;
    }

    public void setJwtExpiresInSeconds(long jwtExpiresInSeconds) {
        this.jwtExpiresInSeconds = jwtExpiresInSeconds;
    }

    public long getRefreshTokenExpiresInSeconds() {
        return refreshTokenExpiresInSeconds;
    }

    public void setRefreshTokenExpiresInSeconds(long refreshTokenExpiresInSeconds) {
        this.refreshTokenExpiresInSeconds = refreshTokenExpiresInSeconds;
    }
}
