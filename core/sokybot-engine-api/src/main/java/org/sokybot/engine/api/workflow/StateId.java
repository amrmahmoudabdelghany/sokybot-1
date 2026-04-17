package org.sokybot.engine.api.workflow;

import java.util.Objects;

public final class StateId implements Comparable<StateId> {
    private final String canonical;

    private StateId(String canonical) {
        this.canonical = canonical;
    }

    public static StateId of(String value) {
        String normalized = normalize(value);
        return new StateId(normalized);
    }

    public static StateId ofNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return new StateId(trimmed);
    }

    public String asString() {
        return canonical;
    }

    @Override
    public int compareTo(StateId other) {
        return canonical.compareTo(other.canonical);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StateId)) return false;
        StateId other = (StateId) obj;
        return Objects.equals(canonical, other.canonical);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canonical);
    }

    @Override
    public String toString() {
        return canonical;
    }

    private static String normalize(String value) {
        String normalized = Objects.requireNonNull(value, "stateId").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("StateId cannot be empty");
        }
        return normalized;
    }
}
