package com.recruit.agent.privacy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RecursiveSensitiveDataRedactorTest {

    private final SensitiveDataRedactor redactor = new RecursiveSensitiveDataRedactor();

    @Test
    void redactsEverySupportedSensitiveFragmentWithoutRetainingPlaintext() {
        String source = """
                phone=13812345678 email=hr@example.com id=11010519491231002X \
                key=sk-1041da71bd7d41038c5258a6365d23e0 \
                Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature
                """;

        String result = redactor.redactText(source);

        assertEquals("""
                phone=[REDACTED_PHONE] email=[REDACTED_EMAIL] id=[REDACTED_ID_CARD] \
                key=[REDACTED_API_KEY] \
                Authorization: Bearer [REDACTED_BEARER_TOKEN]
                """, result);
        assertFalse(result.contains("13812345678"));
        assertFalse(result.contains("hr@example.com"));
        assertFalse(result.contains("11010519491231002X"));
        assertFalse(result.contains("1041da71"));
        assertFalse(result.contains("eyJhbGci"));
    }

    @Test
    void redactsLabeledApiKeysThatDoNotUseKnownPrefixes() {
        assertEquals(
                "provider api_key=[REDACTED_API_KEY], retry=true",
                redactor.redactText("provider api_key=provider-secret-2026, retry=true"));
        assertEquals(
                "apiKey: '[REDACTED_API_KEY]'",
                redactor.redactText("apiKey: 'custom.secret.value'"));
    }

    @Test
    void recursivelyCopiesAndRedactsMapsAndLists() {
        List<Object> messages = new ArrayList<>();
        messages.add("contact 13900001111");
        messages.add(Map.of(
                "email", "candidate@example.org",
                "authorization", "Bearer should-never-remain"));

        Map<String, Object> source = new LinkedHashMap<>();
        source.put("candidateId", 42L);
        source.put("messages", messages);
        source.put("apiKey", "custom-provider-secret");

        Object resultValue = redactor.redact(source);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resultValue;
        assertNotSame(source, result);
        assertEquals(42L, result.get("candidateId"));
        assertEquals(RecursiveSensitiveDataRedactor.REDACTED_API_KEY, result.get("apiKey"));

        @SuppressWarnings("unchecked")
        List<Object> sanitizedMessages = (List<Object>) result.get("messages");
        assertNotSame(messages, sanitizedMessages);
        assertEquals("contact [REDACTED_PHONE]", sanitizedMessages.get(0));
        assertEquals(Map.of(
                        "email", RecursiveSensitiveDataRedactor.REDACTED_EMAIL,
                        "authorization", RecursiveSensitiveDataRedactor.REDACTED_BEARER_TOKEN),
                sanitizedMessages.get(1));

        assertEquals("contact 13900001111", messages.get(0));
        assertEquals("custom-provider-secret", source.get("apiKey"));
    }

    @Test
    void sensitiveFieldNamesAreNormalizedAndListValuesRemainStructured() {
        Map<String, Object> source = Map.of(
                "client_secret", "clear-secret",
                "access-token", List.of("first-token", "second-token"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) redactor.redact(source);

        assertEquals(RecursiveSensitiveDataRedactor.REDACTED_API_KEY, result.get("client_secret"));
        assertEquals(List.of(
                        RecursiveSensitiveDataRedactor.REDACTED_BEARER_TOKEN,
                        RecursiveSensitiveDataRedactor.REDACTED_BEARER_TOKEN),
                result.get("access-token"));
    }

    @Test
    void leavesNonSensitiveContentAndUnsupportedTypesUnchanged() {
        String ordinaryText = "candidate 42 has 8 years of Java experience";
        Object marker = new Object();

        assertEquals(ordinaryText, redactor.redact(ordinaryText));
        assertSame(marker, redactor.redact(marker));
        assertEquals("order 12345678901234567890", redactor.redactText("order 12345678901234567890"));
        assertEquals("phone-like 12812345678", redactor.redactText("phone-like 12812345678"));
    }

    @Test
    void handlesNullAndEmptyValues() {
        assertNull(redactor.redact(null));
        assertNull(redactor.redactText(null));
        assertEquals("", redactor.redactText(""));
    }

    @Test
    void redactsArraysAndGeneralCollections() {
        assertEquals(
                List.of(RecursiveSensitiveDataRedactor.REDACTED_EMAIL,
                        RecursiveSensitiveDataRedactor.REDACTED_PHONE),
                redactor.redact(new String[]{"person@example.com", "13812345678"})
        );
        assertEquals(
                List.of(RecursiveSensitiveDataRedactor.REDACTED_API_KEY),
                redactor.redact(java.util.Set.of("sk-1234567890123456"))
        );
    }
}
