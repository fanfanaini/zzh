package com.recruit.agent.conversation.domain;

public interface ConversationIntentClassifier {

    IntentClassificationResult classify(IntentClassificationRequest request);
}
