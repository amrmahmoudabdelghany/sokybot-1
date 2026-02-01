package org.sokybot.commons.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable error information record.
 * Captures all relevant information about an application error.
 */
public class ErrorInfo {

    private final Instant timestamp;
    private final ErrorCategory category;
    private final String source;
    private final String message;
    private final String machineId;
    private final String stackTrace;

    private ErrorInfo(Builder builder) {
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.category = Objects.requireNonNull(builder.category, "category must not be null");
        this.source = Objects.requireNonNull(builder.source, "source must not be null");
        this.message = Objects.requireNonNull(builder.message, "message must not be null");
        this.machineId = builder.machineId;
        this.stackTrace = builder.stackTrace;
    }

    /**
     * Creates a new builder for ErrorInfo.
     * 
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an ErrorInfo from an exception.
     * 
     * @param category  error category
     * @param source    source component
     * @param exception the exception
     * @return new ErrorInfo instance
     */
    public static ErrorInfo fromException(ErrorCategory category, String source, Throwable exception) {
        return builder()
                .category(category)
                .source(source)
                .message(exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName())
                .stackTrace(getStackTraceString(exception))
                .build();
    }

    /**
     * Creates an ErrorInfo from an exception with machine context.
     * 
     * @param category  error category
     * @param source    source component
     * @param machineId machine identifier
     * @param exception the exception
     * @return new ErrorInfo instance
     */
    public static ErrorInfo fromException(ErrorCategory category, String source, String machineId,
            Throwable exception) {
        return builder()
                .category(category)
                .source(source)
                .machineId(machineId)
                .message(exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName())
                .stackTrace(getStackTraceString(exception))
                .build();
    }

    private static String getStackTraceString(Throwable exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // Getters

    public Instant getTimestamp() {
        return timestamp;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ErrorInfo{");
        sb.append("timestamp=").append(timestamp);
        sb.append(", category=").append(category);
        sb.append(", source='").append(source).append('\'');
        sb.append(", message='").append(message).append('\'');
        if (machineId != null) {
            sb.append(", machineId='").append(machineId).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Builder for ErrorInfo.
     */
    public static class Builder {
        private Instant timestamp;
        private ErrorCategory category;
        private String source;
        private String message;
        private String machineId;
        private String stackTrace;

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder category(ErrorCategory category) {
            this.category = category;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder machineId(String machineId) {
            this.machineId = machineId;
            return this;
        }

        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public ErrorInfo build() {
            return new ErrorInfo(this);
        }
    }
}
