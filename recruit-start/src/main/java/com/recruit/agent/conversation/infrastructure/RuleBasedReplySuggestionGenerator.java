package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ReplyIntent;
import com.recruit.agent.conversation.domain.ReplySuggestionDraft;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.job.domain.Job;

import java.util.List;

public class RuleBasedReplySuggestionGenerator implements ReplySuggestionGenerator {

    @Override
    public ReplySuggestionDraft generate(
            Job job,
            Candidate candidate,
            Conversation conversation,
            List<ConversationMessage> messages
    ) {
        String latest = messages.isEmpty() ? "" : messages.get(messages.size() - 1).content().toLowerCase();
        if (latest.contains("薪") || latest.contains("salary")) {
            return new ReplySuggestionDraft(
                    ReplyIntent.ASK_SALARY,
                    0.82,
                    true,
                    "您好，" + job.name() + " 岗位的薪资会结合面试表现和岗位匹配度综合评估。方便的话，也请您先告知期望薪资范围，HR 会进一步确认。"
            );
        }
        if (latest.contains("时间") || latest.contains("面试")) {
            return new ReplySuggestionDraft(
                    ReplyIntent.INVITE_INTERVIEW,
                    0.86,
                    true,
                    "您好，我们这边初步认为您的经历和 " + job.name() + " 比较匹配。请问您近期方便安排一次线上沟通吗？"
            );
        }
        return new ReplySuggestionDraft(
                ReplyIntent.ASK_INTENTION,
                0.78,
                false,
                "您好，看到您的经历与我们正在招聘的 " + job.name() + " 有一定匹配度，想了解一下您近期是否在看新的工作机会？"
        );
    }
}
