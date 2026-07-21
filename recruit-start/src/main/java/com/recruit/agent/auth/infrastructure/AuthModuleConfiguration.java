package com.recruit.agent.auth.infrastructure;

import com.recruit.agent.auth.application.AuthApplicationService;
import com.recruit.agent.auth.application.JwtService;
import com.recruit.agent.auth.domain.UserAccountRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthModuleConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtService jwtService(AuthProperties properties) {
        return new JwtService(
                properties.getJwtSecret(),
                properties.getJwtExpiresInSeconds(),
                properties.getRefreshTokenExpiresInSeconds(),
                Clock.systemUTC()
        );
    }

    @Bean
    public AuthApplicationService authApplicationService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        return new AuthApplicationService(userAccountRepository, passwordEncoder, jwtService);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }
}
