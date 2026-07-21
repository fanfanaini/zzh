package com.recruit.agent.evaluation.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMemory;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationRoute;
import com.recruit.agent.conversation.domain.ConversationStage;
import com.recruit.agent.conversation.domain.ConversationStatus;
import com.recruit.agent.conversation.domain.IntentClassificationRequest;
import com.recruit.agent.conversation.domain.MessageSenderType;
import com.recruit.agent.conversation.infrastructure.DeterministicConversationRoutingPolicy;
import com.recruit.agent.conversation.infrastructure.RuleBasedConversationIntentClassifier;
import com.recruit.agent.evaluation.domain.EvaluationCase;
import com.recruit.agent.evaluation.domain.EvaluationReport;
import com.recruit.agent.evaluation.domain.EvaluationResult;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.screening.domain.EducationLevel;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AgentCoreBaselineService {
    public static final String DATASET_RESOURCE = "evaluation/recruitment-agent-core-v2.jsonl";

    private final EvaluationJsonlParser parser;
    private final AgentEvaluationService evaluationService;
    private final RuleBasedConversationIntentClassifier classifier = new RuleBasedConversationIntentClassifier();
    private final DeterministicConversationRoutingPolicy routingPolicy = new DeterministicConversationRoutingPolicy();

    public AgentCoreBaselineService(EvaluationJsonlParser parser, AgentEvaluationService evaluationService) {
        this.parser = parser;
        this.evaluationService = evaluationService;
    }

    public EvaluationReport run(InputStream casesInput) throws IOException {
        List<EvaluationCase> cases = parser.parseCases(casesInput);
        List<EvaluationResult> results = cases.stream().map(this::execute).toList();
        return evaluationService.evaluate(cases, results, 5);
    }

    private EvaluationResult execute(EvaluationCase evaluationCase) {
        long started = System.nanoTime();
        var classification = classifier.classify(new IntentClassificationRequest(
                job(), candidate(), conversation(), memory(evaluationCase.message())));
        ConversationRoute route = routingPolicy.route(classification);
        String selectedTool = selectedTool(route);
        boolean handoff = classification.handoffRequired()
                || route == ConversationRoute.HUMAN_HANDOFF
                || route == ConversationRoute.INTERVIEW_SCHEDULING;
        boolean taskSuccess = classification.intent()
                != com.recruit.agent.conversation.domain.ReplyIntent.UNKNOWN;
        long latencyMillis = Math.max(0, (System.nanoTime() - started) / 1_000_000);
        return new EvaluationResult(EvaluationResult.SCHEMA_VERSION, evaluationCase.datasetVersion(),
                evaluationCase.caseId(), classification.intent().name(), route.name(), selectedTool, List.of(),
                taskSuccess, handoff, latencyMillis, null, null, null, null);
    }

    private String selectedTool(ConversationRoute route) {
        return switch (route) {
            case JOB_INFO -> "get_job_info";
            case CANDIDATE_STATUS -> "get_candidate_status";
            case FAQ_RAG -> "search_knowledge";
            default -> EvaluationCase.NO_TOOL;
        };
    }

    private ConversationMemory memory(String message) {
        ConversationMessage candidateMessage = new ConversationMessage(4L, 3L,
                MessageSenderType.CANDIDATE, message, Instant.EPOCH);
        return new ConversationMemory(List.of(candidateMessage), "", Map.of(), Map.of(), message.length(), false);
    }

    private Job job() {
        return new Job(1L, "Java backend", "Backend development", Set.of("java"), Set.of("redis"),
                EducationLevel.BACHELOR, 1, 5, Instant.EPOCH);
    }

    private Candidate candidate() {
        return new Candidate(2L, "candidate", "phone", "email", EducationLevel.BACHELOR, 2,
                "Chengdu", Set.of("java"), Instant.EPOCH);
    }

    private Conversation conversation() {
        return new Conversation(3L, 1L, 2L, ConversationStage.INITIAL_CONTACT,
                ConversationStatus.OPEN, Instant.EPOCH);
    }
}
