package org.sokybot.town.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default immutable {@link IIntent} for intent sources and tests.
 */
public final class Intent implements IIntent {

    private final IntentKind kind;
    private final String reason;
    private final long declaredAtEpochMillis;
    private final String machineFullName;
    private final Map<String, String> metadata;

    private Intent(Builder builder) {
        this.kind = Objects.requireNonNull(builder.kind, "kind");
        this.reason = Objects.requireNonNull(builder.reason, "reason");
        this.declaredAtEpochMillis = builder.declaredAtEpochMillis;
        this.machineFullName = Objects.requireNonNull(builder.machineFullName, "machineFullName");
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
    }

    @Override
    public IntentKind getKind() {
        return kind;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public long getDeclaredAtEpochMillis() {
        return declaredAtEpochMillis;
    }

    @Override
    public String getMachineFullName() {
        return machineFullName;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private IntentKind kind;
        private String reason = "";
        private long declaredAtEpochMillis;
        private String machineFullName = "";
        private final Map<String, String> metadata = new LinkedHashMap<>();

        public Builder kind(IntentKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder declaredAtEpochMillis(long declaredAtEpochMillis) {
            this.declaredAtEpochMillis = declaredAtEpochMillis;
            return this;
        }

        public Builder machineFullName(String machineFullName) {
            this.machineFullName = machineFullName;
            return this;
        }

        public Builder putMetadata(String key, String value) {
            if (key != null && value != null) {
                this.metadata.put(key, value);
            }
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata.clear();
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        public Intent build() {
            return new Intent(this);
        }
    }
}
