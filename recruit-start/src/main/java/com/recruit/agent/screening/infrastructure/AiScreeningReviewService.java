package com.recruit.agent.screening.infrastructure;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.domain.LlmStructuredRequest;
import com.recruit.agent.screening.domain.ScreeningRecommendation;
import com.recruit.agent.screening.domain.ScreeningResult;
import com.recruit.agent.screening.domain.ScreeningReviewRequest;
import com.recruit.agent.screening.domain.ScreeningReviewService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AiScreeningReviewService implements ScreeningReviewService {

    private final LlmService llmService;

    public AiScreeningReviewService(LlmService llmService) {
        this.llmService = llmService;
    }

    @Override
    public ScreeningResult review(ScreeningReviewRequest request) {
        if (request.ruleResult().recommendation() == ScreeningRecommendation.NOT_MATCH) {
            return request.ruleResult();
        }
        try {
            AiScreeningReviewResult review = llmService.structuredChat(new LlmStructuredRequest<>(
                    "SCREENING_REVIEW",
                    "screening-review-v1",
                    """
                            你是招聘筛选复核助手。
                            你只能根据输入中的岗位、候选人画像、规则分和语义分输出判断。
                            没有证据时只能说“未发现”，不能推断候选人不会某项技能。
                            不得根据年龄、性别、婚育、籍贯等非岗位必要因素判断。
                            输出 JSON。recommendation 只能是 INTERVIEW, MANUAL_REVIEW, NOT_MATCH。
                            """,
                    """
                            岗位：
                            - 名称：%s
                            - 描述：%s
                            - 必须技能：%s
                            - 加分技能：%s
                            - 学历要求：%s
                            - 最低年限：%s

                            候选人：
                            - 姓名：%s
                            - 学历：%s
                            - 年限：%s
                            - 城市：%s
                            - 技能：%s

                            规则筛选结果：
                            - recommendation：%s
                            - skillScore：%s
                            - projectScore：%s
                            - experienceScore：%s
                            - educationScore：%s
                            - bonusScore：%s
                            - finalScore：%s
                            - advantages：%s
                            - risks：%s
                            - missingInformation：%s

                            语义相似度分：%s

                            请输出：
                            {
                              "recommendation": "INTERVIEW",
                              "confidence": 0.86,
                              "advantages": ["..."],
                              "risks": ["..."],
                              "missingInformation": ["..."],
                              "suggestedQuestions": ["..."]
                            }
                            """.formatted(
                            request.job().name(),
                            request.job().description(),
                            request.job().requiredSkills(),
                            request.job().preferredSkills(),
                            request.job().educationRequirement(),
                            request.job().minExperienceYears(),
                            request.candidate().name(),
                            request.candidate().educationLevel(),
                            request.candidate().totalExperienceYears(),
                            request.candidate().expectedCity(),
                            request.candidate().skills(),
                            request.ruleResult().recommendation(),
                            request.ruleResult().score().skillScore(),
                            request.ruleResult().score().projectScore(),
                            request.ruleResult().score().experienceScore(),
                            request.ruleResult().score().educationScore(),
                            request.ruleResult().score().bonusScore(),
                            request.ruleResult().score().finalScore(),
                            request.ruleResult().advantages(),
                            request.ruleResult().risks(),
                            request.ruleResult().missingInformation(),
                            request.semanticScore()
                    ),
                    AiScreeningReviewResult.class
            ));
            return merge(request.ruleResult(), review);
        } catch (RuntimeException exception) {
            return request.ruleResult();
        }
    }

    private ScreeningResult merge(ScreeningResult ruleResult, AiScreeningReviewResult review) {
        List<String> advantages = mergeLists(ruleResult.advantages(), review.advantages());
        List<String> risks = mergeLists(ruleResult.risks(), review.risks());
        List<String> missing = mergeLists(ruleResult.missingInformation(), review.missingInformation());
        if (review.suggestedQuestions() != null && !review.suggestedQuestions().isEmpty()) {
            missing = new ArrayList<>(missing);
            review.suggestedQuestions().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> "建议追问: " + value.trim())
                    .forEach(missing::add);
        }
        return new ScreeningResult(
                parseRecommendation(review.recommendation(), ruleResult.recommendation()),
                ruleResult.score(),
                advantages,
                risks,
                missing
        );
    }

    private List<String> mergeLists(List<String> left, List<String> right) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        if (left != null) {
            left.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).forEach(values::add);
        }
        if (right != null) {
            right.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).forEach(values::add);
        }
        return List.copyOf(values);
    }

    private ScreeningRecommendation parseRecommendation(String value, ScreeningRecommendation fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return ScreeningRecommendation.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
