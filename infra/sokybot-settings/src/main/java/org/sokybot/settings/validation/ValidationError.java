package org.sokybot.settings.validation;

import java.util.Objects;

/**
 * Represents a single validation error for a settings field.
 */
public class ValidationError {

    private final String fieldName;
    private final String message;
    private final String constraint;
    private final Object rejectedValue;

    public ValidationError(String fieldName, String message, String constraint, Object rejectedValue) {
        this.fieldName = Objects.requireNonNull(fieldName, "fieldName must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
        this.constraint = constraint;
        this.rejectedValue = rejectedValue;
    }

    /**
     * Create a validation error.
     */
    public static ValidationError of(String fieldName, String message, String constraint) {
        return new ValidationError(fieldName, message, constraint, null);
    }

    /**
     * Create a validation error with rejected value.
     */
    public static ValidationError of(String fieldName, String message, String constraint, Object rejectedValue) {
        return new ValidationError(fieldName, message, constraint, rejectedValue);
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getMessage() {
        return message;
    }

    public String getConstraint() {
        return constraint;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    @Override
    public String toString() {
        return fieldName + ": " + message;
    }
}
