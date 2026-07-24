package com.recruit.agent.privacy;

/**
 * Removes secrets and personal identifiers before values are logged, traced, or persisted.
 */
public interface SensitiveDataRedactor {

    /**
     * Recursively redacts supported values. Strings, maps, and lists are copied and redacted;
     * other value types are returned unchanged.
     *
     * @param value value to sanitize
     * @return a sanitized copy for container values, or the original non-container value
     */
    Object redact(Object value);

    /**
     * Redacts sensitive fragments embedded in a string.
     *
     * @param value text to sanitize
     * @return sanitized text, or {@code null} when the input is {@code null}
     */
    String redactText(String value);
}
