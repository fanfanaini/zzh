package com.recruit.agent.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.ai.domain.LlmCallResult;
import com.recruit.agent.ai.domain.RawLlmClient;

import java.util.Map;

public class MockRawLlmClient implements RawLlmClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LlmCallResult structuredChat(String systemPrompt, String userPrompt) {
        long started = System.currentTimeMillis();
        String response = responseFor(userPrompt);
        return new LlmCallResult(modelName(), response, null, null, System.currentTimeMillis() - started);
    }

    @Override
    public String modelName() {
        return "mock-llm";
    }

    private String responseFor(String userPrompt) {
        try {
            if (userPrompt.contains("简历原文")) {
                return objectMapper.writeValueAsString(Map.ofEntries(
                        Map.entry("educationLevel", "BACHELOR"),
                        Map.entry("totalExperienceYears", 2.0),
                        Map.entry("expectedCity", "成都"),
                        Map.entry("skills", java.util.List.of("java", "spring boot", "mysql")),
                        Map.entry("educationExperiences", java.util.List.of(Map.of(
                                "school", "示例大学",
                                "degree", "本科",
                                "major", "软件工程",
                                "sourceText", "本科 软件工程",
                                "confidence", 0.9
                        ))),
                        Map.entry("workExperiences", java.util.List.of()),
                        Map.entry("projectExperiences", java.util.List.of()),
                        Map.entry("certificates", java.util.List.of()),
                        Map.entry("languages", java.util.List.of()),
                        Map.entry("awards", java.util.List.of()),
                        Map.entry("fieldSources", Map.of("educationLevel", "本科")),
                        Map.entry("fieldConfidence", Map.of("educationLevel", 0.9))
                ));
            }
            return objectMapper.writeValueAsString(Map.of(
                    "intent", "ASK_INTENTION",
                    "confidence", 0.76,
                    "needHuman", false,
                    "suggestedReply", "您好，看到您的经历与岗位有一定匹配度，想了解一下您近期是否在看新的工作机会？"
            ));
        } catch (Exception exception) {
            throw new IllegalStateException("Mock LLM 响应生成失败", exception);
        }
    }
}
