package org.sokybot.commons.health;

import java.time.Instant;
import java.util.Objects;

/**
 * Result of a health check execution.
 * Immutable value object.
 */
public class HealthCheckResult {

    private final String componentName;
    private final HealthCategory category;
    private final HealthStatus status;
    private final String message;
    private final Instant timestamp;

    private HealthCheckResult(Builder builder) {
        this.componentName = Objects.requireNonNull(builder.componentName, "componentName must not be null");
        this.category = Objects.requireNonNull(builder.category, "category must not be null");
        this.status = Objects.requireNonNull(builder.status, "status must not be null");
        this.message = builder.message;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
    }

    /**
     * Creates a healthy result.
     */
    public static HealthCheckResult healthy(String componentName, HealthCategory category) {
        return builder()
                .componentName(componentName)
                .category(category)
                .status(HealthStatus.HEALTHY)
                .build();
    }

    /**
     * Creates a healthy result with a message.
     */
    public static HealthCheckResult healthy(String componentName, HealthCategory category, String message) {
        return builder()
                .componentName(componentName)
                .category(category)
                .status(HealthStatus.HEALTHY)
                .message(message)
                .build();
    }

    /**
     * Creates a degraded result.
     */
    public static HealthCheckResult degraded(String componentName, HealthCategory category, String message) {
        return builder()
                .componentName(componentName)
                .category(category)
                .status(HealthStatus.DEGRADED)
                .message(message)
                .build();
    }

    /**
     * Creates an unhealthy result.
     */
    public static HealthCheckResult unhealthy(String componentName, HealthCategory category, String message) {
        return builder()
                .componentName(componentName)
                .category(category)
                .status(HealthStatus.UNHEALTHY)
                .message(message)
                .build();
    }

    /**
     * Creates an unhealthy result from an exception.
     */
    public static HealthCheckResult unhealthy(String componentName, HealthCategory category, Throwable exception) {
        String msg = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();
        return unhealthy(componentName, category, msg);
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters

    public String getComponentName() {
        return componentName;
    }

    public HealthCategory getCategory() {
        return category;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HealthCheckResult{");
        sb.append("component='").append(componentName).append('\'');
        sb.append(", status=").append(status);
        if (message != null) {
            sb.append(", message='").append(message).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Builder for HealthCheckResult.
     */
    public static class Builder {
        private String componentName;
        private HealthCategory category;
        private HealthStatus status;
        private String message;
        private Instant timestamp;

        public Builder componentName(String componentName) {
            this.componentName = componentName;
            return this;
        }

        public Builder category(HealthCategory category) {
            this.category = category;
            return this;
        }

        public Builder status(HealthStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public HealthCheckResult build() {
            return new HealthCheckResult(this);
        }
    }
}
