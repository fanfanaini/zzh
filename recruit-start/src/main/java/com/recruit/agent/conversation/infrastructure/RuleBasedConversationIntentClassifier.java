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

        if (contains(message, "\u6295\u8bc9", "\u4e3e\u62a5", "\u6b67\u89c6", "\u8fdd\u6cd5", "\u52b3\u52a8\u4ef2\u88c1", "complaint", "illegal")) {
            return result(ReplyIntent.OUT_OF_DOMAIN, 0.95,
                    Set.of(ConversationRisk.COMPLAINT_OR_LEGAL), true, "Sensitive complaint or legal issue");
        }
        if (contains(message, "\u8eab\u4efd\u8bc1", "\u94f6\u884c\u5361", "\u5bb6\u5ead\u4f4f\u5740", "id card", "bank account")) {
            return result(ReplyIntent.OUT_OF_DOMAIN, 0.95,
                    Set.of(ConversationRisk.SENSITIVE_PERSONAL_DATA), true, "Sensitive personal data");
        }
        if (contains(message, "\u5f55\u7528", "offer", "\u6dd8\u6c70", "\u4fdd\u8bc1\u5165\u804c", "\u4e00\u5b9a\u901a\u8fc7")) {
            return result(ReplyIntent.REJECT_CANDIDATE, 0.9,
                    Set.of(ConversationRisk.EMPLOYMENT_DECISION), true, "Employment decision requires HR");
        }
        if (contains(message, "\u85aa\u8d44", "\u5de5\u8d44", "\u6708\u85aa", "\u5e74\u85aa", "salary", "compensation")) {
            return result(ReplyIntent.ASK_SALARY, 0.9,
                    Set.of(ConversationRisk.SALARY_COMMITMENT), true, "Salary commitment requires HR");
        }
        if (contains(message, "\u9762\u8bd5\u65f6\u95f4", "\u9884\u7ea6\u9762\u8bd5", "\u6539\u671f", "\u51e0\u70b9\u9762\u8bd5", "interview time", "reschedule")) {
            return result(ReplyIntent.ASK_AVAILABILITY, 0.9, Set.of(), false, "Interview scheduling request");
        }
        if (contains(message, "\u8fdb\u5ea6", "\u7ed3\u679c", "\u72b6\u6001", "\u6709\u6d88\u606f", "application status", "result")) {
            return result(ReplyIntent.ASK_CANDIDATE_STATUS, 0.86, Set.of(), false, "Candidate status request");
        }
        if (contains(message, "\u5c97\u4f4d\u804c\u8d23", "\u804c\u4f4d\u8981\u6c42", "\u5de5\u4f5c\u5185\u5bb9", "\u6280\u672f\u6808", "job description", "requirements")) {
            return result(ReplyIntent.ANSWER_JOB_QUESTION, 0.88, Set.of(), false, "Job information request");
        }
        if (contains(message, "\u798f\u5229", "\u5047\u671f", "\u793e\u4fdd", "\u516c\u79ef\u91d1", "\u52a0\u73ed", "benefit", "leave policy")) {
            return result(ReplyIntent.ASK_FAQ, 0.86, Set.of(), false, "Recruiting policy question");
        }
        if (contains(message, "\u4f60\u597d", "\u60a8\u597d", "hello", "hi ", "hi!")) {
            return result(ReplyIntent.GREETING, 0.85, Set.of(), false, "Greeting");
        }
        if (contains(message, "\u5929\u6c14", "\u80a1\u7968", "\u7535\u5f71", "\u6e38\u620f\u653b\u7565", "weather", "stock price", "movie")) {
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
