package org.sokybot.settings.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of validating a settings object.
 */
public class ValidationResult {

    private final List<ValidationError> errors;

    private ValidationResult(List<ValidationError> errors) {
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Create a successful validation result.
     */
    public static ValidationResult valid() {
        return new ValidationResult(Collections.emptyList());
    }

    /**
     * Create a failed validation result.
     */
    public static ValidationResult invalid(List<ValidationError> errors) {
        return new ValidationResult(errors);
    }

    /**
     * Create a failed validation result with single error.
     */
    public static ValidationResult invalid(ValidationError error) {
        return new ValidationResult(Collections.singletonList(error));
    }

    /**
     * Check if validation passed.
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Get validation errors.
     */
    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * Get first error message, or null if valid.
     */
    public String getFirstErrorMessage() {
        return errors.isEmpty() ? null : errors.get(0).getMessage();
    }

    @Override
    public String toString() {
        if (isValid()) {
            return "ValidationResult{valid=true}";
        }
        return "ValidationResult{valid=false, errors=" + errors.size() + "}";
    }
}
