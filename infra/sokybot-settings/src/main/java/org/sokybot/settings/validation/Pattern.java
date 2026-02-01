package org.sokybot.settings.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string field matches the specified regex pattern.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Pattern {

    /**
     * Regular expression pattern.
     */
    String value();

    /**
     * Error message if validation fails.
     */
    String message() default "Value does not match required pattern";
}
