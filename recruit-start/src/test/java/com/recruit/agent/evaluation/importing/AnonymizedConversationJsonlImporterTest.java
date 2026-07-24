package com.recruit.agent.evaluation.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.evaluation.domain.EvaluationCase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnonymizedConversationJsonlImporterTest {

    private final AnonymizedConversationJsonlImporter importer =
            new AnonymizedConversationJsonlImporter(new ObjectMapper());

    @Test
    void importsStrictAnonymizedConversationsAsExistingEvaluationCases() throws Exception {
        Path fixture = Path.of("src/test/resources/evaluation/anonymized-conversations-v1.jsonl");

        var cases = importer.importCases(fixture);

        assertThat(cases).hasSize(2);
        EvaluationCase first = cases.get(0);
        assertThat(first.schemaVersion()).isEqualTo(EvaluationCase.SCHEMA_VERSION);
        assertThat(first.datasetVersion()).isEqualTo("real-conversations/2026-07");
        assertThat(first.caseId()).isEqualTo("case-001");
        assertThat(first.message()).isEqualTo("请帮我查看匹配这个岗位的候选人");
        assertThat(first.expectedIntent()).isEqualTo("SCREENING");
        assertThat(first.relevantDocumentIds()).containsExactly("kb-job-screening");
        assertThat(first.tags()).containsExactly("real-dialogue", "screening");
    }

    @Test
    void rejectsRawCandidatePiiFieldsAndValuesWithoutReturningARecord() {
        assertRejected(recordWith("\"candidateName\":\"张三\","),
                "PII field is forbidden: candidateName");
        assertRejected(recordWithTurn("请联系 candidate@example.com"), "raw email address");
        assertRejected(recordWithTurn("手机号是 138-0013-8000"), "raw phone number");
        assertRejected(recordWithTurn("候选人姓名：张三"), "candidate-name marker");
        assertRejected(recordWithTurn("身份证 11010519491231002X"), "raw identity number");
        assertRejected(recordWithTurn("token sk-1041da71bd7d41038c5258a6365d23e0"), "secret credential");
        assertRejected(recordWithTurn("Authorization Bearer private.token.value"), "secret credential");
        assertRejected(record("13800138000", "real-conversations/2026-07", "请筛选候选人"),
                "caseId contains a raw phone number");
    }

    @Test
    void rejectsUnknownFieldsDuplicateJsonKeysAndInvalidConversationShape() {
        assertRejected(recordWith("\"sourceSystem\":\"crm\","),
                "unknown field at record: sourceSystem");
        assertRejected(recordWith("\"caseId\":\"duplicate-key\","),
                "Duplicate field 'caseId'");
        assertRejected(recordWithTurns("""
                [{"role":"USER","content":"第一问"},{"role":"USER","content":"第二问"}]
                """), "turn roles must alternate");
        assertRejected(recordWithTurns("""
                [{"role":"USER","content":"问题"},{"role":"ASSISTANT","content":"回答"}]
                """), "final turn must have role USER");
    }

    @Test
    void reportsSourceLineAndRejectsMixedDatasetOrDuplicateCaseIds() {
        String valid = record("case-001", "real-conversations/2026-07", "问题一");
        String duplicate = record("case-001", "real-conversations/2026-07", "问题二");
        String mixed = record("case-002", "real-conversations/2026-08", "问题二");

        assertThatThrownBy(() -> importer.importCases(bytes(valid + System.lineSeparator() + duplicate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line 2")
                .hasMessageContaining("duplicate caseId");
        assertThatThrownBy(() -> importer.importCases(bytes(valid + System.lineSeparator() + mixed)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line 2")
                .hasMessageContaining("datasetVersion must be identical");
    }

    @Test
    void rejectsBlankDataset() {
        assertThatThrownBy(() -> importer.importCases(bytes(System.lineSeparator())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JSONL input contains no records");
    }

    private void assertRejected(String jsonl, String message) {
        assertThatThrownBy(() -> importer.importCases(bytes(jsonl)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line 1")
                .hasMessageContaining(message);
    }

    private String recordWith(String additionalField) {
        return oneLine("""
                {
                  "schemaVersion":"anonymized-conversation-evaluation/v1",
                  "datasetVersion":"real-conversations/2026-07",
                  "caseId":"case-001",
                  %s
                  "conversationId":"conversation-001",
                  "provenance":"production-sample",
                  "anonymized":true,
                  "piiPolicy":"candidate-pii-removed/v1",
                  "turns":[{"role":"USER","content":"请筛选候选人"}],
                  "expectedIntent":"SCREENING",
                  "expectedRoute":"SCREENING",
                  "expectedTool":"screen_candidates",
                  "expectedTaskSuccess":true,
                  "expectedHandoff":false
                }
                """.formatted(additionalField));
    }

    private String recordWithTurn(String content) {
        return recordWithTurns("[{\"role\":\"USER\",\"content\":\"" + content + "\"}]");
    }

    private String recordWithTurns(String turns) {
        return oneLine("""
                {
                  "schemaVersion":"anonymized-conversation-evaluation/v1",
                  "datasetVersion":"real-conversations/2026-07",
                  "caseId":"case-001",
                  "conversationId":"conversation-001",
                  "provenance":"production-sample",
                  "anonymized":true,
                  "piiPolicy":"candidate-pii-removed/v1",
                  "turns":%s,
                  "expectedIntent":"SCREENING",
                  "expectedRoute":"SCREENING",
                  "expectedTool":"screen_candidates",
                  "expectedTaskSuccess":true,
                  "expectedHandoff":false
                }
                """.formatted(turns));
    }

    private String record(String caseId, String datasetVersion, String content) {
        return """
                {"schemaVersion":"anonymized-conversation-evaluation/v1","datasetVersion":"%s","caseId":"%s","conversationId":"conversation-001","provenance":"production-sample","anonymized":true,"piiPolicy":"candidate-pii-removed/v1","turns":[{"role":"USER","content":"%s"}],"expectedIntent":"SCREENING","expectedRoute":"SCREENING","expectedTool":"screen_candidates","expectedTaskSuccess":true,"expectedHandoff":false}
                """.formatted(datasetVersion, caseId, content).strip();
    }

    private ByteArrayInputStream bytes(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private String oneLine(String value) {
        return value.replaceAll("\\R\\s*", "").strip();
    }
}
