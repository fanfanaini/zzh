package com.recruit.agent.skill.infrastructure;

import com.recruit.agent.skill.application.RecruitmentSkillApplicationService;
import com.recruit.agent.skill.application.RecruitmentSkillMatcher;
import com.recruit.agent.skill.application.RecruitmentSkillProperties;
import com.recruit.agent.skill.application.SkillContextAssembler;
import com.recruit.agent.skill.domain.RecruitmentSkillRepository;
import com.recruit.agent.trace.UnifiedTraceRecorder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RecruitmentSkillProperties.class)
public class RecruitmentSkillModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RecruitmentSkillRepository recruitmentSkillRepository() {
        return new InMemoryRecruitmentSkillRepository();
    }

    @Bean
    public RecruitmentSkillMatcher recruitmentSkillMatcher() {
        return new RecruitmentSkillMatcher();
    }

    @Bean
    public RecruitmentSkillApplicationService recruitmentSkillApplicationService(
            RecruitmentSkillRepository repository,
            RecruitmentSkillMatcher matcher,
            RecruitmentSkillProperties properties
    ) {
        return new RecruitmentSkillApplicationService(repository, matcher, properties);
    }

    @Bean
    public SkillContextAssembler skillContextAssembler(RecruitmentSkillRepository repository,
                                                       RecruitmentSkillMatcher matcher,
                                                       RecruitmentSkillProperties properties,
                                                       ObjectProvider<UnifiedTraceRecorder> traceRecorder) {
        return new SkillContextAssembler(repository, matcher, properties, traceRecorder.getIfAvailable());
    }

    @Bean
    public MarkdownSkillImporter markdownSkillImporter() {
        return new MarkdownSkillImporter();
    }
}
