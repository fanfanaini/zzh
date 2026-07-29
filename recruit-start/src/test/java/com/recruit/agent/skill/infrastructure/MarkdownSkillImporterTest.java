package com.recruit.agent.skill.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownSkillImporterTest {

    @Test
    void parsesFrontMatterAndSections() {
        String markdown = """
                ---
                name: salary_expectation_reply
                category: conversation
                description: Salary replies
                scenario: Candidate asks salary
                keywords: salary,薪资
                allowedTools: get_job_info,search_knowledge
                riskTags: salary_commitment
                priority: 90
                version: v2
                publish: true
                ---
                ## Instruction
                Do not promise salary.

                ## Prompt Fragment
                Ask for expectation.

                ## Guardrails
                Do not invent numbers.
                """;

        var command = new MarkdownSkillImporter().parse(markdown);

        assertThat(command.name()).isEqualTo("salary_expectation_reply");
        assertThat(command.triggerKeywords()).contains("salary", "薪资");
        assertThat(command.allowedTools()).contains("get_job_info", "search_knowledge");
        assertThat(command.riskTags()).contains("salary_commitment");
        assertThat(command.instruction()).isEqualTo("Do not promise salary.");
        assertThat(command.version()).isEqualTo("v2");
        assertThat(command.publish()).isTrue();
    }
}
