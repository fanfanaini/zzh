package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMemory;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationRisk;
import com.recruit.agent.conversation.domain.ConversationStage;
import com.recruit.agent.conversation.domain.ConversationStatus;
import com.recruit.agent.conversation.domain.IntentClassificationRequest;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.conversation.domain.ReplyIntent;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedConversationIntentClassifierTest {

    private final RuleBasedConversationIntentClassifier classifier = new RuleBasedConversationIntentClassifier();

    @Test
    void classifiesJobInformationAndCandidateStatus() {
        assertThat(classify("这个岗位职责和技术栈是什么？").intent()).isEqualTo(ReplyIntent.ANSWER_JOB_QUESTION);
        assertThat(classify("请问我的面试进度有消息了吗？").intent()).isEqualTo(ReplyIntent.ASK_CANDIDATE_STATUS);
    }

    @Test
    void marksSalaryAndLegalComplaintsForHumanHandoff() {
        var salary = classify("你们能保证月薪三万吗？");
        var complaint = classify("我要投诉招聘歧视并申请劳动仲裁");

        assertThat(salary.intent()).isEqualTo(ReplyIntent.ASK_SALARY);
        assertThat(salary.handoffRequired()).isTrue();
        assertThat(salary.risks()).contains(ConversationRisk.SALARY_COMMITMENT);
        assertThat(complaint.intent()).isEqualTo(ReplyIntent.OUT_OF_DOMAIN);
        assertThat(complaint.risks()).contains(ConversationRisk.COMPLAINT_OR_LEGAL);
    }

    @Test
    void classifiesClearlyUnrelatedQuestionAsOutOfDomain() {
        assertThat(classify("今天成都天气怎么样？"))
                .extracting("intent", "handoffRequired")
                .containsExactly(ReplyIntent.OUT_OF_DOMAIN, false);
    }

    private com.recruit.agent.conversation.domain.IntentClassificationResult classify(String text) {
        ConversationMessage message = new ConversationMessage(1L, 3L, MessageSenderType.CANDIDATE, text, Instant.now());
        ConversationMemory memory = new ConversationMemory(List.of(message), "", Map.of(), Map.of(), text.length(), false);
        return classifier.classify(new IntentClassificationRequest(job(), candidate(), conversation(), memory));
    }

    private Job job() {
        return new Job(1L, "Java 后端", "后端开发", Set.of("java"), Set.of(),
                EducationLevel.BACHELOR, 1, 3, Instant.EPOCH);
    }

    private Candidate candidate() {
        return new Candidate(2L, "张三", "p", "e", EducationLevel.BACHELOR, 2,
                "成都", Set.of("java"), Instant.EPOCH);
    }

    private Conversation conversation() {
        return new Conversation(3L, 1L, 2L, ConversationStage.INITIAL_CONTACT,
                ConversationStatus.OPEN, Instant.EPOCH);
    }
}
