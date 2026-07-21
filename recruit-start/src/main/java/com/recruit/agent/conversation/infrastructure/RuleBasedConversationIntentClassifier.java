package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.conversation.domain.ConversationIntentClassifier;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationRisk;
import com.recruit.agent.conversation.domain.IntentClassificationRequest;
import com.recruit.agent.conversation.domain.IntentClassificationResult;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.conversation.domain.ReplyIntent;

import java.util.Locale;
import java.util.Set;

public class RuleBasedConversationIntentClassifier implements ConversationIntentClassifier {

    @Override
    public IntentClassificationResult classify(IntentClassificationRequest request) {
        String message = request.memory().recentMessages().stream()
                .filter(item -> item.senderType() == MessageSenderType.CANDIDATE)
                .reduce((first, second) -> second)
                .map(ConversationMessage::content)
                .orElse("")
                .toLowerCase(Locale.ROOT);

        if (contains(message, "投诉", "举报", "歧视", "违法", "劳动仲裁", "complaint", "illegal")) {
            return result(ReplyIntent.OUT_OF_DOMAIN, 0.95,
                    Set.of(ConversationRisk.COMPLAINT_OR_LEGAL), true, "Sensitive complaint or legal issue");
        }
        if (contains(message, "身份证", "银行卡", "家庭住址", "id card", "bank account")) {
            return result(ReplyIntent.OUT_OF_DOMAIN, 0.95,
                    Set.of(ConversationRisk.SENSITIVE_PERSONAL_DATA), true, "Sensitive personal data");
        }
        if (contains(message, "录用", "offer", "淘汰", "保证入职", "一定通过")) {
            return result(ReplyIntent.REJECT_CANDIDATE, 0.9,
                    Set.of(ConversationRisk.EMPLOYMENT_DECISION), true, "Employment decision requires HR");
        }
        if (contains(message, "薪资", "工资", "月薪", "年薪", "salary", "compensation")) {
            return result(ReplyIntent.ASK_SALARY, 0.9,
                    Set.of(ConversationRisk.SALARY_COMMITMENT), true, "Salary commitment requires HR");
        }
        if (contains(message, "面试时间", "预约面试", "改期", "几点面试", "interview time", "reschedule")) {
            return result(ReplyIntent.ASK_AVAILABILITY, 0.9, Set.of(), false, "Interview scheduling request");
        }
        if (contains(message, "进度", "结果", "状态", "有消息", "application status", "result")) {
            return result(ReplyIntent.ASK_CANDIDATE_STATUS, 0.86, Set.of(), false, "Candidate status request");
        }
        if (contains(message, "岗位职责", "职位要求", "工作内容", "技术栈", "job description", "requirements")) {
            return result(ReplyIntent.ANSWER_JOB_QUESTION, 0.88, Set.of(), false, "Job information request");
        }
        if (contains(message, "福利", "假期", "社保", "公积金", "加班", "benefit", "leave policy")) {
            return result(ReplyIntent.ASK_FAQ, 0.86, Set.of(), false, "Recruiting policy question");
        }
        if (contains(message, "你好", "您好", "hello", "hi ", "hi!")) {
            return result(ReplyIntent.GREETING, 0.85, Set.of(), false, "Greeting");
        }
        if (contains(message, "天气", "股票", "电影", "游戏攻略", "weather", "stock price", "movie")) {
            return result(ReplyIntent.OUT_OF_DOMAIN, 0.9, Set.of(), false, "Outside recruiting domain");
        }
        return result(ReplyIntent.UNKNOWN, 0.55, Set.of(), false, "No deterministic rule matched");
    }

    private IntentClassificationResult result(ReplyIntent intent, double confidence,
                                              Set<ConversationRisk> risks, boolean handoff, String reason) {
        return new IntentClassificationResult(intent, confidence, risks, handoff, reason);
    }

    private boolean contains(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
