package com.recruit.agent.evaluation.importing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.evaluation.domain.EvaluationCase;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Imports pre-anonymized conversation JSONL into the current single-turn evaluation format.
 */
@Component
public final class AnonymizedConversationJsonlImporter {

    public static final String SCHEMA_VERSION = "anonymized-conversation-evaluation/v1";
    public static final String PII_POLICY = "candidate-pii-removed/v1";

    private static final int MAX_LINE_LENGTH = 64 * 1024;
    private static final int MAX_TURNS = 100;
    private static final int MAX_CONTENT_LENGTH = 8_000;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)(?<![\\p{L}\\p{N}._%+-])[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}.-]+\\.[A-Za-z]{2,}(?![\\p{L}\\p{N}])");
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)(?:\\+?\\d[\\s().-]*){7,15}(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile(
            "(?<!\\d)(?:\\d{17}[0-9Xx]|\\d{15})(?!\\d)");
    private static final Pattern API_KEY = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_-])(?:sk|pk)-[A-Za-z0-9][A-Za-z0-9_-]{11,}(?![A-Za-z0-9_-])");
    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]+={0,2}");
    private static final Pattern EXPLICIT_NAME = Pattern.compile(
            "(?i)(?:姓名|候选人姓名|我叫|名字(?:是|叫)|name\\s*(?:is|:)|candidate\\s*name\\s*:)");

    private static final Set<String> ROOT_FIELDS = Set.of(
            "schemaVersion", "datasetVersion", "caseId", "conversationId", "provenance",
            "anonymized", "piiPolicy", "turns", "expectedIntent", "expectedRoute", "expectedTool",
            "relevantDocumentIds", "expectedTaskSuccess", "expectedHandoff", "tags");
    private static final Set<String> TURN_FIELDS = Set.of("role", "content");
    private static final Set<String> FORBIDDEN_FIELD_NAMES = Set.of(
            "name", "candidatename", "fullname", "realname", "phone", "phonenumber",
            "mobile", "mobilephone", "email", "emailaddress");

    private final ObjectMapper objectMapper;

    public AnonymizedConversationJsonlImporter(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("objectMapper must not be null");
        }
        this.objectMapper = objectMapper.copy()
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    public List<EvaluationCase> importCases(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return importCases(input);
        }
    }

    public List<EvaluationCase> importCases(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        List<EvaluationCase> cases = new ArrayList<>();
        Set<String> caseIds = new HashSet<>();
        String datasetVersion = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                if (line.length() > MAX_LINE_LENGTH) {
                    throw invalid(lineNumber, "record exceeds " + MAX_LINE_LENGTH + " characters", null);
                }
                try {
                    JsonNode root = objectMapper.readTree(line);
                    EvaluationCase evaluationCase = convert(root);
                    if (!caseIds.add(evaluationCase.caseId())) {
                        throw new IllegalArgumentException("duplicate caseId: " + evaluationCase.caseId());
                    }
                    if (datasetVersion == null) {
                        datasetVersion = evaluationCase.datasetVersion();
                    } else if (!datasetVersion.equals(evaluationCase.datasetVersion())) {
                        throw new IllegalArgumentException("datasetVersion must be identical for every record");
                    }
                    cases.add(evaluationCase);
                } catch (JsonProcessingException | IllegalArgumentException exception) {
                    throw invalid(lineNumber, exception.getMessage(), exception);
                }
            }
        }
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("JSONL input contains no records");
        }
        return List.copyOf(cases);
    }

    private EvaluationCase convert(JsonNode root) {
        requireObject(root, "record");
        rejectForbiddenFields(root);
        requireOnlyFields(root, ROOT_FIELDS, "record");
        requireExactText(root, "schemaVersion", SCHEMA_VERSION);
        requireBoolean(root, "anonymized", true);
        requireExactText(root, "piiPolicy", PII_POLICY);

        String datasetVersion = requireText(root, "datasetVersion");
        String caseId = requireSafeId(root, "caseId");
        requireSafeId(root, "conversationId");
        String provenance = requireText(root, "provenance");
        String expectedIntent = requireText(root, "expectedIntent");
        String expectedRoute = requireText(root, "expectedRoute");
        String expectedTool = requireText(root, "expectedTool");
        boolean expectedTaskSuccess = requireBoolean(root, "expectedTaskSuccess");
        boolean expectedHandoff = requireBoolean(root, "expectedHandoff");
        List<String> relevantDocumentIds = optionalStringArray(root, "relevantDocumentIds", true);
        List<String> tags = optionalStringArray(root, "tags", false);
        String message = lastUserMessage(root);

        scanForPii(datasetVersion, "datasetVersion");
        scanForPii(provenance, "provenance");
        scanForPii(message, "turns.content");
        scanForPii(expectedIntent, "expectedIntent");
        scanForPii(expectedRoute, "expectedRoute");
        scanForPii(expectedTool, "expectedTool");
        tags.forEach(value -> scanForPii(value, "tags"));

        return new EvaluationCase(
                EvaluationCase.SCHEMA_VERSION,
                datasetVersion,
                caseId,
                provenance,
                message,
                expectedIntent,
                expectedRoute,
                expectedTool,
                relevantDocumentIds,
                expectedTaskSuccess,
                expectedHandoff,
                tags);
    }

    private String lastUserMessage(JsonNode root) {
        JsonNode turns = root.get("turns");
        if (turns == null || !turns.isArray() || turns.isEmpty()) {
            throw new IllegalArgumentException("turns must be a non-empty array");
        }
        if (turns.size() > MAX_TURNS) {
            throw new IllegalArgumentException("turns must contain at most " + MAX_TURNS + " items");
        }

        String lastUserMessage = null;
        String previousRole = null;
        for (int index = 0; index < turns.size(); index++) {
            JsonNode turn = turns.get(index);
            requireObject(turn, "turns[" + index + "]");
            rejectForbiddenFields(turn);
            requireOnlyFields(turn, TURN_FIELDS, "turns[" + index + "]");
            String role = requireText(turn, "role").toUpperCase(Locale.ROOT);
            if (!role.equals("USER") && !role.equals("ASSISTANT")) {
                throw new IllegalArgumentException("turns[" + index + "].role must be USER or ASSISTANT");
            }
            if (role.equals(previousRole)) {
                throw new IllegalArgumentException("turn roles must alternate");
            }
            String content = requireText(turn, "content");
            if (content.length() > MAX_CONTENT_LENGTH) {
                throw new IllegalArgumentException(
                        "turns[" + index + "].content exceeds " + MAX_CONTENT_LENGTH + " characters");
            }
            scanForPii(content, "turns[" + index + "].content");
            if (role.equals("USER")) {
                lastUserMessage = content;
            }
            previousRole = role;
        }
        if (!"USER".equals(previousRole)) {
            throw new IllegalArgumentException("the final turn must have role USER");
        }
        return lastUserMessage;
    }

    private void rejectForbiddenFields(JsonNode node) {
        if (node.isObject()) {
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                String normalized = name.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
                if (FORBIDDEN_FIELD_NAMES.contains(normalized)) {
                    throw new IllegalArgumentException("PII field is forbidden: " + name);
                }
                rejectForbiddenFields(node.get(name));
            }
        } else if (node.isArray()) {
            node.forEach(this::rejectForbiddenFields);
        }
    }

    private void scanForPii(String value, String field) {
        if (EMAIL.matcher(value).find()) {
            throw new IllegalArgumentException(field + " contains a raw email address");
        }
        if (PHONE.matcher(value).find()) {
            throw new IllegalArgumentException(field + " contains a raw phone number");
        }
        if (ID_CARD.matcher(value).find()) {
            throw new IllegalArgumentException(field + " contains a raw identity number");
        }
        if (API_KEY.matcher(value).find() || BEARER_TOKEN.matcher(value).find()) {
            throw new IllegalArgumentException(field + " contains a secret credential");
        }
        if (EXPLICIT_NAME.matcher(value).find()) {
            throw new IllegalArgumentException(field + " contains an unredacted candidate-name marker");
        }
    }

    private void requireOnlyFields(JsonNode node, Set<String> allowed, String location) {
        node.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException("unknown field at " + location + ": " + field);
            }
        });
    }

    private void requireObject(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(field + " must be an object");
        }
    }

    private String requireSafeId(JsonNode node, String field) {
        String value = requireText(node, field);
        if (!SAFE_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be an anonymized identifier");
        }
        scanForPii(value, field);
        return value;
    }

    private String requireText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException(field + " must be a non-blank string");
        }
        return value.textValue();
    }

    private void requireExactText(JsonNode node, String field, String expected) {
        String actual = requireText(node, field);
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("unsupported " + field + ": " + actual);
        }
    }

    private boolean requireBoolean(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isBoolean()) {
            throw new IllegalArgumentException(field + " must be a boolean");
        }
        return value.booleanValue();
    }

    private void requireBoolean(JsonNode node, String field, boolean expected) {
        if (requireBoolean(node, field) != expected) {
            throw new IllegalArgumentException(field + " must be " + expected);
        }
    }

    private List<String> optionalStringArray(JsonNode node, String field, boolean safeIds) {
        JsonNode values = node.get(field);
        if (values == null) {
            return List.of();
        }
        if (!values.isArray()) {
            throw new IllegalArgumentException(field + " must be an array");
        }
        List<String> result = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            JsonNode value = values.get(index);
            if (!value.isTextual() || value.textValue().isBlank()) {
                throw new IllegalArgumentException(field + "[" + index + "] must be a non-blank string");
            }
            String text = value.textValue();
            if (safeIds && !SAFE_ID.matcher(text).matches()) {
                throw new IllegalArgumentException(field + "[" + index + "] must be an anonymized identifier");
            }
            if (safeIds) {
                scanForPii(text, field + "[" + index + "]");
            }
            if (!unique.add(text)) {
                throw new IllegalArgumentException(field + " must not contain duplicates");
            }
            result.add(text);
        }
        return List.copyOf(result);
    }

    private IllegalArgumentException invalid(int lineNumber, String message, Exception cause) {
        String detail = message == null || message.isBlank() ? "invalid record" : message;
        return new IllegalArgumentException("Invalid anonymized conversation JSONL at line "
                + lineNumber + ": " + detail, cause);
    }
}
