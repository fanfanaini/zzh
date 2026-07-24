package com.recruit.agent.privacy;

import java.util.ArrayList;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless recursive redactor suitable for request payloads, agent traces, and diagnostic logs.
 */
public final class RecursiveSensitiveDataRedactor implements SensitiveDataRedactor {

    public static final String REDACTED_PHONE = "[REDACTED_PHONE]";
    public static final String REDACTED_EMAIL = "[REDACTED_EMAIL]";
    public static final String REDACTED_ID_CARD = "[REDACTED_ID_CARD]";
    public static final String REDACTED_API_KEY = "[REDACTED_API_KEY]";
    public static final String REDACTED_BEARER_TOKEN = "[REDACTED_BEARER_TOKEN]";

    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]+={0,2}");
    private static final Pattern LABELED_API_KEY = Pattern.compile(
            "(?i)(\\bapi[ _-]?key\\s*[:=]\\s*[\"']?)[^\\s,;}\"']+");
    private static final Pattern PREFIXED_API_KEY = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_-])(?:sk|pk)-[A-Za-z0-9][A-Za-z0-9_-]{11,}(?![A-Za-z0-9_-])");
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)(?<![A-Z0-9.!#$%&'*+/^_`{|}~-])"
                    + "[A-Z0-9.!#$%&'*+/^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?"
                    + "(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+(?![A-Z0-9-])");
    private static final Pattern ID_CARD = Pattern.compile(
            "(?<!\\d)(?:\\d{17}[0-9Xx]|\\d{15})(?!\\d)");
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)1[3-9]\\d{9}(?!\\d)");

    private static final Set<String> API_KEY_FIELDS = Set.of(
            "apikey", "apisecret", "secretkey", "clientsecret");
    private static final Set<String> BEARER_TOKEN_FIELDS = Set.of(
            "authorization", "accesstoken", "bearertoken");

    @Override
    public Object redact(Object value) {
        return redactValue(value, null);
    }

    @Override
    public String redactText(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String redacted = BEARER_TOKEN.matcher(value)
                .replaceAll("Bearer " + REDACTED_BEARER_TOKEN);
        redacted = replaceLabeledApiKeys(redacted);
        redacted = PREFIXED_API_KEY.matcher(redacted).replaceAll(REDACTED_API_KEY);
        redacted = EMAIL.matcher(redacted).replaceAll(REDACTED_EMAIL);
        redacted = ID_CARD.matcher(redacted).replaceAll(REDACTED_ID_CARD);
        return PHONE.matcher(redacted).replaceAll(REDACTED_PHONE);
    }

    private Object redactValue(Object value, Object fieldName) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            String fieldReplacement = replacementForField(fieldName);
            return fieldReplacement == null ? redactText(text) : fieldReplacement;
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>(map.size());
            map.forEach((key, child) -> copy.put(
                    key,
                    redactValue(child, replacementForField(fieldName) == null ? key : fieldName)));
            return copy;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> copy = new ArrayList<>(collection.size());
            for (Object child : collection) {
                copy.add(redactValue(child, fieldName));
            }
            return copy;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> copy = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                copy.add(redactValue(Array.get(value, index), fieldName));
            }
            return copy;
        }
        return value;
    }

    private String replacementForField(Object fieldName) {
        if (!(fieldName instanceof String name)) {
            return null;
        }
        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (BEARER_TOKEN_FIELDS.contains(normalized)) {
            return REDACTED_BEARER_TOKEN;
        }
        if (API_KEY_FIELDS.contains(normalized)) {
            return REDACTED_API_KEY;
        }
        return null;
    }

    private String replaceLabeledApiKeys(String value) {
        Matcher matcher = LABELED_API_KEY.matcher(value);
        StringBuffer result = new StringBuffer(value.length());
        while (matcher.find()) {
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(matcher.group(1) + REDACTED_API_KEY));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
