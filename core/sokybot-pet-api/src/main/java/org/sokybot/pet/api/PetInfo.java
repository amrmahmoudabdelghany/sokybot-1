package org.sokybot.pet.api;

import java.util.Objects;

/**
 * Immutable snapshot of one active COS pet instance for a machine.
 */
public final class PetInfo {

    private final int entityUniqueId;
    private final int objectId;
    private final PetRole role;
    private final int hp;
    private final int maxHp;
    /** Server hunger points; {@code -1} if unknown. */
    private final int hunger;
    private final boolean alive;
    private final long lastUpdateEpochMs;

    private PetInfo(Builder b) {
        this.entityUniqueId = b.entityUniqueId;
        this.objectId = b.objectId;
        this.role = b.role != null ? b.role : PetRole.UNKNOWN;
        this.hp = b.hp;
        this.maxHp = b.maxHp;
        this.hunger = b.hunger;
        this.alive = b.alive;
        this.lastUpdateEpochMs = b.lastUpdateEpochMs;
    }

    public int getEntityUniqueId() {
        return entityUniqueId;
    }

    public int getObjectId() {
        return objectId;
    }

    public PetRole getRole() {
        return role;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getHunger() {
        return hunger;
    }

    public boolean isAlive() {
        return alive;
    }

    public long getLastUpdateEpochMs() {
        return lastUpdateEpochMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PetInfo petInfo = (PetInfo) o;
        return entityUniqueId == petInfo.entityUniqueId
                && objectId == petInfo.objectId
                && hp == petInfo.hp
                && maxHp == petInfo.maxHp
                && hunger == petInfo.hunger
                && alive == petInfo.alive
                && lastUpdateEpochMs == petInfo.lastUpdateEpochMs
                && role == petInfo.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityUniqueId, objectId, role, hp, maxHp, hunger, alive, lastUpdateEpochMs);
    }

    @Override
    public String toString() {
        return "PetInfo{entityUniqueId=" + entityUniqueId + ", objectId=" + objectId + ", role=" + role
                + ", hp=" + hp + "/" + maxHp + ", hunger=" + hunger + ", alive=" + alive
                + ", lastUpdateEpochMs=" + lastUpdateEpochMs + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int entityUniqueId;
        private int objectId;
        private PetRole role = PetRole.UNKNOWN;
        private int hp;
        private int maxHp;
        private int hunger = -1;
        private boolean alive = true;
        private long lastUpdateEpochMs;

        public Builder entityUniqueId(int entityUniqueId) {
            this.entityUniqueId = entityUniqueId;
            return this;
        }

        public Builder objectId(int objectId) {
            this.objectId = objectId;
            return this;
        }

        public Builder role(PetRole role) {
            this.role = role;
            return this;
        }

        public Builder hp(int hp) {
            this.hp = hp;
            return this;
        }

        public Builder maxHp(int maxHp) {
            this.maxHp = maxHp;
            return this;
        }

        public Builder hunger(int hunger) {
            this.hunger = hunger;
            return this;
        }

        public Builder alive(boolean alive) {
            this.alive = alive;
            return this;
        }

        public Builder lastUpdateEpochMs(long lastUpdateEpochMs) {
            this.lastUpdateEpochMs = lastUpdateEpochMs;
            return this;
        }

        public PetInfo build() {
            return new PetInfo(this);
        }
    }
}
