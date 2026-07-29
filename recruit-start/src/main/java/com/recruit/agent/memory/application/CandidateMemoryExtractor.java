package com.recruit.agent.memory.application;

import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.memory.domain.CandidateMemoryFact;
import com.recruit.agent.memory.domain.CandidateMemorySourceType;
import com.recruit.agent.memory.domain.CandidateMemoryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CandidateMemoryExtractor {
    private static final Pattern SALARY_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*[kK千wW万]?\\s*[-~到至]\\s*(\\d+(?:\\.\\d+)?)\\s*[kK千wW万]?");
    private static final Pattern NOTICE_PATTERN = Pattern.compile("(?:离职|到岗|入职|notice|交接).*?(\\d+)\\s*(?:天|周|个月|month|week|day)s?");
    private static final Pattern CITY_PATTERN = Pattern.compile("(?:希望|期望|考虑|base|地点|城市).*?([\\p{IsHan}A-Za-z]{2,20})(?:工作|办公|base|城市|岗位|机会)?");

    public List<CandidateMemoryFact> extract(Conversation conversation, ConversationMessage message) {
        if (conversation == null || message == null || message.content() == null) {
            return List.of();
        }
        String content = message.content().trim();
        if (content.isBlank()) {
            return List.of();
        }
        String lower = content.toLowerCase(Locale.ROOT);
        List<CandidateMemoryFact> facts = new ArrayList<>();
        Long sourceId = message.id();

        Matcher salary = SALARY_PATTERN.matcher(content);
        if (containsAny(lower, "薪资", "工资", "待遇", "salary", "compensation", "期望") && salary.find()) {
            facts.add(fact(conversation.candidateId(), CandidateMemoryType.PREFERENCE,
                    "salary_expectation", salary.group(), 0.82, sourceId, content));
        }
        if (containsAny(lower, "远程", "remote", "居家")) {
            facts.add(fact(conversation.candidateId(), CandidateMemoryType.PREFERENCE,
                    "work_mode", "prefers remote or hybrid flexibility", 0.74, sourceId, content));
        }
        if (containsAny(lower, "不考虑", "暂不考虑", "没兴趣", "不方便")) {
            facts.add(fact(conversation.candidateId(), CandidateMemoryType.STATUS,
                    "current_interest", "low or temporarily unavailable", 0.72, sourceId, content));
        }
        if (containsAny(lower, "有兴趣", "可以聊", "方便", "感兴趣", "愿意")) {
            facts.add(fact(conversation.candidateId(), CandidateMemoryType.STATUS,
                    "current_interest", "interested and open to follow-up", 0.72, sourceId, content));
        }
        if (containsAny(lower, "投诉", "隐私", "举报", "歧视", "违法", "劳动仲裁", "威胁")) {
            facts.add(fact(conversation.candidateId(), CandidateMemoryType.RISK,
                    "communication_risk", "sensitive or complaint-related communication", 0.86, sourceId, content));
        }
        Matcher notice = NOTICE_PATTERN.matcher(lower);
        if (notice.find()) {
            facts.add(fact(conversation.candidateId(), CandidateMemoryType.STATUS,
                    "availability_notice", notice.group(), 0.78, sourceId, content));
        }
        Matcher city = CITY_PATTERN.matcher(content);
        if (containsAny(lower, "城市", "地点", "base", "上海", "北京", "深圳", "杭州", "广州") && city.find()) {
            String cityValue = city.group(1);
            if (!isWeakCity(cityValue)) {
                facts.add(fact(conversation.candidateId(), CandidateMemoryType.PREFERENCE,
                        "preferred_city", cityValue, 0.68, sourceId, content));
            }
        }
        return facts;
    }

    private CandidateMemoryFact fact(Long candidateId, CandidateMemoryType type, String key, String value,
                                     double confidence, Long sourceId, String evidence) {
        return new CandidateMemoryFact(-1L, candidateId, type, key, value, confidence,
                CandidateMemorySourceType.CONVERSATION_MESSAGE, sourceId, clip(evidence));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean isWeakCity(String value) {
        return value == null || value.length() < 2 || value.contains("希望") || value.contains("考虑");
    }

    private String clip(String value) {
        if (value == null) return "";
        return value.length() <= 300 ? value : value.substring(0, 300);
    }
}
