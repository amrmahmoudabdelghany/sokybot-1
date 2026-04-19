package org.sokybot.town.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Next step toward leaving town and resuming the hunt ({@link IReturnRouteStrategy}).
 */
public final class ReturnAction {

    private final ReturnActionKind kind;
    private final Map<String, String> parameters;

    private ReturnAction(Builder builder) {
        this.kind = Objects.requireNonNull(builder.kind, "kind");
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(builder.parameters));
    }

    public ReturnActionKind getKind() {
        return kind;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ReturnActionKind kind;
        private final Map<String, String> parameters = new LinkedHashMap<>();

        public Builder kind(ReturnActionKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder putParameter(String key, String value) {
            if (key != null && value != null) {
                this.parameters.put(key, value);
            }
            return this;
        }

        public Builder parameters(Map<String, String> parameters) {
            this.parameters.clear();
            if (parameters != null) {
                this.parameters.putAll(parameters);
            }
            return this;
        }

        public ReturnAction build() {
            return new ReturnAction(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReturnAction)) {
            return false;
        }
        ReturnAction that = (ReturnAction) o;
        return kind == that.kind && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, parameters);
    }
}
