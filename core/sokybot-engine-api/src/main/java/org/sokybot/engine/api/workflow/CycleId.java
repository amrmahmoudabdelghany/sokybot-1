package org.sokybot.engine.api.workflow;

import java.util.Objects;

public final class CycleId {
    private final String canonical;

    private CycleId(String canonical) {
        this.canonical = canonical;
    }

    public static CycleId of(String value) {
        String normalized = Objects.requireNonNull(value, "cycleId").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("CycleId cannot be empty");
        }
        return new CycleId(normalized);
    }

    public String asString() {
        return canonical;
    }

    public StateId qualify(StateId stateId) {
        Objects.requireNonNull(stateId, "stateId");
        return StateId.of(canonical + "." + stateId.asString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CycleId)) return false;
        CycleId other = (CycleId) obj;
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
}
