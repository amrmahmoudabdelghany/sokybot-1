package org.sokybot.settings.api;

import java.util.List;
import java.util.Map;

import org.sokybot.settings.validation.ValidationResult;

/**
 * Service for validating settings objects and generating schemas.
 * 
 * Uses reflection to process validation annotations on settings fields.
 */
public interface ISettingsValidator {

    /**
     * Validate a settings object.
     * 
     * @param settings the settings object to validate
     * @return validation result with any errors
     */
    ValidationResult validate(Object settings);

    /**
     * Get the schema for a settings class.
     * Schema includes field metadata for UI rendering.
     * 
     * @param settingsClass the settings class
     * @return list of field schemas
     */
    List<Map<String, Object>> getSchema(Class<?> settingsClass);

    /**
     * Check if a settings class has validation annotations.
     * 
     * @param settingsClass the settings class
     * @return true if the class has validation annotations
     */
    boolean hasValidation(Class<?> settingsClass);
}
