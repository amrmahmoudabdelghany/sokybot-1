package org.sokybot.settings.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a numeric field is at most the specified maximum.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Max {

    /**
     * Maximum value (inclusive).
     */
    long value();

    /**
     * Error message if validation fails.
     */
    String message() default "Value must be at most {value}";
}
