package org.sokybot.settings.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides metadata for UI rendering of a settings field.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SettingsField {

    /**
     * Display label for the field.
     */
    String label() default "";

    /**
     * Description/help text for the field.
     */
    String description() default "";

    /**
     * Display order (lower = first).
     */
    int order() default 100;

    /**
     * Whether to hide this field in the UI.
     */
    boolean hidden() default false;

    /**
     * Field group name (for organizing related fields).
     */
    String group() default "General";
}
