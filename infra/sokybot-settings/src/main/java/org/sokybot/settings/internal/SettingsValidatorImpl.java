package org.sokybot.settings.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import org.osgi.service.component.annotations.Component;
import org.sokybot.settings.api.ISettingsValidator;
import org.sokybot.settings.security.Encrypted;
import org.sokybot.settings.validation.Max;
import org.sokybot.settings.validation.Min;
import org.sokybot.settings.validation.NotEmpty;
import org.sokybot.settings.validation.Pattern;
import org.sokybot.settings.validation.Range;
import org.sokybot.settings.validation.SettingsField;
import org.sokybot.settings.validation.ValidationError;
import org.sokybot.settings.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reflection-based settings validator implementation.
 */
@Component(service = ISettingsValidator.class)
public class SettingsValidatorImpl implements ISettingsValidator {

    private static final Logger log = LoggerFactory.getLogger(SettingsValidatorImpl.class);

    @Override
    public ValidationResult validate(Object settings) {
        if (settings == null) {
            return ValidationResult.invalid(
                    ValidationError.of("settings", "Settings object is null", "NotNull"));
        }

        List<ValidationError> errors = new ArrayList<>();
        Class<?> clazz = settings.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            try {
                Object value = field.get(settings);
                validateField(field, value, errors);
            } catch (IllegalAccessException e) {
                log.warn("Cannot access field {} for validation", field.getName(), e);
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    private void validateField(Field field, Object value, List<ValidationError> errors) {
        String fieldName = field.getName();

        // @NotEmpty
        NotEmpty notEmpty = field.getAnnotation(NotEmpty.class);
        if (notEmpty != null) {
            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                errors.add(ValidationError.of(fieldName, notEmpty.message(), "NotEmpty", value));
            }
        }

        // @Min
        Min min = field.getAnnotation(Min.class);
        if (min != null && value instanceof Number) {
            long numValue = ((Number) value).longValue();
            if (numValue < min.value()) {
                String message = min.message().replace("{value}", String.valueOf(min.value()));
                errors.add(ValidationError.of(fieldName, message, "Min", value));
            }
        }

        // @Max
        Max max = field.getAnnotation(Max.class);
        if (max != null && value instanceof Number) {
            long numValue = ((Number) value).longValue();
            if (numValue > max.value()) {
                String message = max.message().replace("{value}", String.valueOf(max.value()));
                errors.add(ValidationError.of(fieldName, message, "Max", value));
            }
        }

        // @Range
        Range range = field.getAnnotation(Range.class);
        if (range != null && value instanceof Number) {
            long numValue = ((Number) value).longValue();
            if (numValue < range.min() || numValue > range.max()) {
                String message = range.message()
                        .replace("{min}", String.valueOf(range.min()))
                        .replace("{max}", String.valueOf(range.max()));
                errors.add(ValidationError.of(fieldName, message, "Range", value));
            }
        }

        // @Pattern
        Pattern pattern = field.getAnnotation(Pattern.class);
        if (pattern != null && value instanceof String) {
            String strValue = (String) value;
            try {
                if (!strValue.matches(pattern.value())) {
                    errors.add(ValidationError.of(fieldName, pattern.message(), "Pattern", value));
                }
            } catch (PatternSyntaxException e) {
                log.error("Invalid regex pattern in @Pattern for field {}: {}", fieldName, pattern.value());
            }
        }
    }

    @Override
    public List<Map<String, Object>> getSchema(Class<?> settingsClass) {
        List<Map<String, Object>> schema = new ArrayList<>();

        for (Field field : settingsClass.getDeclaredFields()) {
            Map<String, Object> fieldSchema = new LinkedHashMap<>();
            fieldSchema.put("name", field.getName());
            fieldSchema.put("type", getFieldType(field));

            // @SettingsField metadata
            SettingsField sf = field.getAnnotation(SettingsField.class);
            if (sf != null) {
                fieldSchema.put("label", sf.label().isEmpty() ? field.getName() : sf.label());
                if (!sf.description().isEmpty()) {
                    fieldSchema.put("description", sf.description());
                }
                fieldSchema.put("order", sf.order());
                fieldSchema.put("hidden", sf.hidden());
                fieldSchema.put("group", sf.group());
            } else {
                fieldSchema.put("label", field.getName());
                fieldSchema.put("order", 100);
                fieldSchema.put("hidden", false);
                fieldSchema.put("group", "General");
            }

            // @Encrypted
            if (field.isAnnotationPresent(Encrypted.class)) {
                fieldSchema.put("encrypted", true);
            }

            // Validation constraints
            Map<String, Object> constraints = new LinkedHashMap<>();

            if (field.isAnnotationPresent(NotEmpty.class)) {
                constraints.put("required", true);
            }

            Min min = field.getAnnotation(Min.class);
            if (min != null) {
                constraints.put("min", min.value());
            }

            Max max = field.getAnnotation(Max.class);
            if (max != null) {
                constraints.put("max", max.value());
            }

            Range range = field.getAnnotation(Range.class);
            if (range != null) {
                if (range.min() != Long.MIN_VALUE) {
                    constraints.put("min", range.min());
                }
                if (range.max() != Long.MAX_VALUE) {
                    constraints.put("max", range.max());
                }
            }

            Pattern pattern = field.getAnnotation(Pattern.class);
            if (pattern != null) {
                constraints.put("pattern", pattern.value());
            }

            if (!constraints.isEmpty()) {
                fieldSchema.put("constraints", constraints);
            }

            schema.add(fieldSchema);
        }

        // Sort by order
        schema.sort((a, b) -> {
            int orderA = (int) a.getOrDefault("order", 100);
            int orderB = (int) b.getOrDefault("order", 100);
            return Integer.compare(orderA, orderB);
        });

        return schema;
    }

    @Override
    public boolean hasValidation(Class<?> settingsClass) {
        for (Field field : settingsClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(NotEmpty.class)
                    || field.isAnnotationPresent(Min.class)
                    || field.isAnnotationPresent(Max.class)
                    || field.isAnnotationPresent(Range.class)
                    || field.isAnnotationPresent(Pattern.class)) {
                return true;
            }
        }
        return false;
    }

    private String getFieldType(Field field) {
        Class<?> type = field.getType();

        if (type == String.class) {
            return field.isAnnotationPresent(Encrypted.class) ? "password" : "string";
        } else if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class) {
            return "number";
        } else if (type == double.class || type == Double.class
                || type == float.class || type == Float.class) {
            return "decimal";
        } else if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        } else if (type.isEnum()) {
            return "enum";
        }

        return "object";
    }
}
