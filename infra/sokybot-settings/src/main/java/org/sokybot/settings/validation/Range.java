package org.sokybot.settings.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a numeric field is within a specified range.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {

    /**
     * Minimum value (inclusive).
     */
    long min() default Long.MIN_VALUE;

    /**
     * Maximum value (inclusive).
     */
    long max() default Long.MAX_VALUE;

    /**
     * Error message if validation fails.
     */
    String message() default "Value must be between {min} and {max}";
}
