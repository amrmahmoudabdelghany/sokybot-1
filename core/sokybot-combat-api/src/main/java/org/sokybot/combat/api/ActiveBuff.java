package org.sokybot.combat.api;

import java.util.Objects;

/**
 * Immutable snapshot of one active buff effect on the local character (projection-driven).
 */
public final class ActiveBuff {

    private final int buffId;
    private final int skillRefId;
    private final int casterEntityId;
    private final long appliedAtEpochMs;
    /** {@link Long#MAX_VALUE} means permanent or unknown expiry. */
    private final long expiresAtEpochMs;
    private final boolean fromSelf;
    /** True when this buff represents an imbue skill from configuration. */
    private final boolean imbue;

    private ActiveBuff(Builder b) {
        this.buffId = b.buffId;
        this.skillRefId = b.skillRefId;
        this.casterEntityId = b.casterEntityId;
        this.appliedAtEpochMs = b.appliedAtEpochMs;
        this.expiresAtEpochMs = b.expiresAtEpochMs;
        this.fromSelf = b.fromSelf;
        this.imbue = b.imbue;
    }

    public int getBuffId() {
        return buffId;
    }

    public int getSkillRefId() {
        return skillRefId;
    }

    public int getCasterEntityId() {
        return casterEntityId;
    }

    public long getAppliedAtEpochMs() {
        return appliedAtEpochMs;
    }

    public long getExpiresAtEpochMs() {
        return expiresAtEpochMs;
    }

    public boolean isFromSelf() {
        return fromSelf;
    }

    public boolean isImbue() {
        return imbue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActiveBuff activeBuff = (ActiveBuff) o;
        return buffId == activeBuff.buffId
                && skillRefId == activeBuff.skillRefId
                && casterEntityId == activeBuff.casterEntityId
                && appliedAtEpochMs == activeBuff.appliedAtEpochMs
                && expiresAtEpochMs == activeBuff.expiresAtEpochMs
                && fromSelf == activeBuff.fromSelf
                && imbue == activeBuff.imbue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(buffId, skillRefId, casterEntityId, appliedAtEpochMs, expiresAtEpochMs, fromSelf,
                imbue);
    }

    @Override
    public String toString() {
        return "ActiveBuff{buffId=" + buffId + ", skillRefId=" + skillRefId + ", casterEntityId=" + casterEntityId
                + ", appliedAtEpochMs=" + appliedAtEpochMs + ", expiresAtEpochMs=" + expiresAtEpochMs + ", fromSelf="
                + fromSelf + ", imbue=" + imbue + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int buffId;
        private int skillRefId;
        private int casterEntityId;
        private long appliedAtEpochMs;
        private long expiresAtEpochMs = Long.MAX_VALUE;
        private boolean fromSelf;
        private boolean imbue;

        public Builder buffId(int buffId) {
            this.buffId = buffId;
            return this;
        }

        public Builder skillRefId(int skillRefId) {
            this.skillRefId = skillRefId;
            return this;
        }

        public Builder casterEntityId(int casterEntityId) {
            this.casterEntityId = casterEntityId;
            return this;
        }

        public Builder appliedAtEpochMs(long appliedAtEpochMs) {
            this.appliedAtEpochMs = appliedAtEpochMs;
            return this;
        }

        public Builder expiresAtEpochMs(long expiresAtEpochMs) {
            this.expiresAtEpochMs = expiresAtEpochMs;
            return this;
        }

        public Builder fromSelf(boolean fromSelf) {
            this.fromSelf = fromSelf;
            return this;
        }

        public Builder imbue(boolean imbue) {
            this.imbue = imbue;
            return this;
        }

        public ActiveBuff build() {
            return new ActiveBuff(this);
        }
    }
}
