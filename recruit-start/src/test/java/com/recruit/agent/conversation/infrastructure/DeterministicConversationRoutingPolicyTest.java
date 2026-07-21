package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.conversation.domain.ConversationRisk;
import com.recruit.agent.conversation.domain.ConversationRoute;
import com.recruit.agent.conversation.domain.IntentClassificationResult;
import com.recruit.agent.conversation.domain.ReplyIntent;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicConversationRoutingPolicyTest {

    private final DeterministicConversationRoutingPolicy policy = new DeterministicConversationRoutingPolicy();

    @Test
    void routesEverySupportedConversationCapability() {
        assertRoute(ReplyIntent.ASK_FAQ, ConversationRoute.FAQ_RAG);
        assertRoute(ReplyIntent.ANSWER_JOB_QUESTION, ConversationRoute.JOB_INFO);
        assertRoute(ReplyIntent.ASK_AVAILABILITY, ConversationRoute.INTERVIEW_SCHEDULING);
        assertRoute(ReplyIntent.ASK_CANDIDATE_STATUS, ConversationRoute.CANDIDATE_STATUS);
        assertRoute(ReplyIntent.GREETING, ConversationRoute.GENERAL_REPLY);
        assertRoute(ReplyIntent.OUT_OF_DOMAIN, ConversationRoute.SAFE_REPLY);
    }

    @Test
    void riskAlwaysOverridesIntentAndRoutesToHuman() {
        var classification = new IntentClassificationResult(ReplyIntent.ASK_FAQ, 0.9,
                Set.of(ConversationRisk.COMPLAINT_OR_LEGAL), false, "risk");

        assertThat(policy.route(classification)).isEqualTo(ConversationRoute.HUMAN_HANDOFF);
    }

    private void assertRoute(ReplyIntent intent, ConversationRoute expected) {
        var classification = new IntentClassificationResult(intent, 0.9, Set.of(), false, "test");
        assertThat(policy.route(classification)).isEqualTo(expected);
    }
}
