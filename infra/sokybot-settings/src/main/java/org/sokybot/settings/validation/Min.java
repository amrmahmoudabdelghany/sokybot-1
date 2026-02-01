package org.sokybot.settings.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a numeric field is at least the specified minimum.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Min {

    /**
     * Minimum value (inclusive).
     */
    long value();

    /**
     * Error message if validation fails.
     */
    String message() default "Value must be at least {value}";
}
