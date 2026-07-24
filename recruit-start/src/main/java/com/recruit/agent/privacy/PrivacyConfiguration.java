package com.recruit.agent.privacy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrivacyConfiguration {

    @Bean
    public SensitiveDataRedactor sensitiveDataRedactor() {
        return new RecursiveSensitiveDataRedactor();
    }
}
