package com.recruit.agent.candidate.infrastructure;

import com.recruit.agent.ai.infrastructure.DefaultLlmService;
import com.recruit.agent.ai.infrastructure.DisabledRawLlmClient;
import com.recruit.agent.ai.infrastructure.InMemoryModelCallLogRepository;
import com.recruit.agent.ai.infrastructure.MockRawLlmClient;
import com.recruit.agent.ai.domain.LlmCallResult;
import com.recruit.agent.ai.domain.RawLlmClient;
import com.recruit.agent.candidate.domain.CandidateProfileDraft;
import com.recruit.agent.candidate.domain.ResumeProfileExtractionResult;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiResumeProfileExtractorTest {

    @Test
    void fallsBackToResumeRulesWhenAiIsDisabled() {
        AiResumeProfileExtractor extractor = new AiResumeProfileExtractor(
                new DefaultLlmService(new DisabledRawLlmClient(), new InMemoryModelCallLogRepository()),
                new RuleBasedResumeProfileExtractor()
        );

        ResumeProfileExtractionResult result = extractor.extractDetailed(
                "电子科技大学 电子信息 硕士 Java Spring Boot MySQL");

        assertThat(result.profile().educationLevel()).isEqualTo(EducationLevel.MASTER);
        assertThat(result.profile().skills()).contains("java", "spring boot", "mysql");
        assertThat(result.structuredDetail().educationExperiences()).isEmpty();
    }

    @Test
    void acceptsDeepSeekAliasesAndMergesProfileWithResumeEvidence() {
        String response = """
                {
                  "educationLevel": "BACHELOR",
                  "totalExperienceYears": 0.25,
                  "expectedCity": null,
                  "skills": ["mybatis"],
                  "educationExperiences": [],
                  "workExperiences": [{"company":"示例公司","position":"Java实习生","description":"后端开发"}],
                  "projectExperiences": [{"projectName":"MindBridge","role":"后端开发"}],
                  "certificates": [],
                  "languages": [{"language":"英语","proficiency":"CET-6","confidence":0.95}],
                  "awards": [{"name":"国家励志奖学金","confidence":0.95}],
                  "fieldSources": {"educationLevel":"本科","expectedCity":null},
                  "fieldConfidence": {"educationLevel":0.9}
                }
                """;
        RawLlmClient client = new RawLlmClient() {
            @Override
            public LlmCallResult structuredChat(String systemPrompt, String userPrompt) {
                return new LlmCallResult("deepseek-test", response, null, null, 1);
            }

            @Override
            public String modelName() {
                return "deepseek-test";
            }
        };
        AiResumeProfileExtractor extractor = new AiResumeProfileExtractor(
                new DefaultLlmService(client, new InMemoryModelCallLogRepository()),
                new RuleBasedResumeProfileExtractor()
        );

        ResumeProfileExtractionResult result = extractor.extractDetailed(
                "电子科技大学 硕士 成都 Java Spring Boot");

        assertThat(result.profile().educationLevel()).isEqualTo(EducationLevel.MASTER);
        assertThat(result.profile().expectedCity()).isEqualTo("成都");
        assertThat(result.profile().skills()).contains("mybatis", "java", "spring boot");
        assertThat(result.structuredDetail().workExperiences()).singleElement()
                .extracting(ResumeProfileExtractionResult.WorkExperience::title)
                .isEqualTo("Java实习生");
        assertThat(result.structuredDetail().projectExperiences()).singleElement()
                .extracting(ResumeProfileExtractionResult.ProjectExperience::name)
                .isEqualTo("MindBridge");
        assertThat(result.structuredDetail().languages()).containsExactly("英语 (CET-6)");
        assertThat(result.structuredDetail().awards()).containsExactly("国家励志奖学金");
        assertThat(result.structuredDetail().fieldSources()).doesNotContainKey("expectedCity");
    }

    @Test
    void extractsProfileThroughLlm() {
        AiResumeProfileExtractor extractor = new AiResumeProfileExtractor(
                new DefaultLlmService(new MockRawLlmClient(), new InMemoryModelCallLogRepository()),
                new RuleBasedResumeProfileExtractor()
        );

        CandidateProfileDraft draft = extractor.extract("简历原文：Java Spring Boot MySQL 成都 本科 2年经验");

        assertThat(draft.educationLevel()).isEqualTo(EducationLevel.BACHELOR);
        assertThat(draft.skills()).contains("java", "spring boot", "mysql");
    }

    @Test
    void extractsStructuredResumeDetailsThroughLlm() {
        AiResumeProfileExtractor extractor = new AiResumeProfileExtractor(
                new DefaultLlmService(new MockRawLlmClient(), new InMemoryModelCallLogRepository()),
                new RuleBasedResumeProfileExtractor()
        );

        ResumeProfileExtractionResult result = extractor.extractDetailed(
                "简历原文：示例大学 软件工程 本科 Java Spring Boot MySQL 成都 2年经验");

        assertThat(result.structuredDetail().educationExperiences())
                .singleElement()
                .satisfies(education -> {
                    assertThat(education.school()).isEqualTo("示例大学");
                    assertThat(education.confidence()).isEqualTo(0.9);
                });
        assertThat(result.structuredDetail().fieldSources()).containsEntry("educationLevel", "本科");
    }
}
