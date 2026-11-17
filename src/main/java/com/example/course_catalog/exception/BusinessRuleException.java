package com.example.course_catalog.exception;

/**
 * Thrown when an action breaks a business rule
 * (for example: duplicate email, deleting an instructor who still has courses).
 *
 * "field" is optional: if set, the controller can show the message
 * next to that form field (e.g. under the email input).
 */
public class BusinessRuleException extends RuntimeException {

    private final String field;

    public BusinessRuleException(String message) {
        this(null, message);
    }

    public BusinessRuleException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}