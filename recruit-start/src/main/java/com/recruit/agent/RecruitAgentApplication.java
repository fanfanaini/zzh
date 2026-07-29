package com.recruit.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecruitAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecruitAgentApplication.class, args);
    }
}
