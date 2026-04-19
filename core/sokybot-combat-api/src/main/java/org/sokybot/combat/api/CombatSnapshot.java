package org.sokybot.combat.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default immutable implementation of {@link ICombatSnapshot}.
 */
public final class CombatSnapshot implements ICombatSnapshot {

    private final String machineFullName;
    private final long snapshotEpochMs;
    private final Optional<Integer> selfEntityId;
    private final Optional<Integer> currentTargetEntityId;
    private final int currentHp;
    private final int maxHp;
    private final int currentMp;
    private final int maxMp;
    private final float selfX;
    private final float selfY;
    private final float selfZ;
    private final List<MonsterRef> nearbyMonsters;
    private final List<DroppedItemRef> nearbyLoot;
    private final Map<Integer, Long> skillCooldownReadyAtEpochMs;
    private final boolean skillCastInFlight;
    private final Optional<Long> berserkActiveUntilEpochMs;
    private final List<ActiveBuff> activeBuffs;

    private CombatSnapshot(Builder builder) {
        this.machineFullName = Objects.requireNonNull(builder.machineFullName, "machineFullName");
        this.snapshotEpochMs = builder.snapshotEpochMs;
        this.selfEntityId = Optional.ofNullable(builder.selfEntityId);
        this.currentTargetEntityId = Optional.ofNullable(builder.currentTargetEntityId);
        this.currentHp = builder.currentHp;
        this.maxHp = builder.maxHp;
        this.currentMp = builder.currentMp;
        this.maxMp = builder.maxMp;
        this.selfX = builder.selfX;
        this.selfY = builder.selfY;
        this.selfZ = builder.selfZ;
        this.nearbyMonsters = Collections.unmodifiableList(
                Objects.requireNonNull(builder.nearbyMonsters, "nearbyMonsters"));
        this.nearbyLoot = Collections.unmodifiableList(
                Objects.requireNonNull(builder.nearbyLoot, "nearbyLoot"));
        this.skillCooldownReadyAtEpochMs = Collections.unmodifiableMap(
                new HashMap<>(Objects.requireNonNull(builder.skillCooldownReadyAtEpochMs, "skillCooldowns")));
        this.skillCastInFlight = builder.skillCastInFlight;
        this.berserkActiveUntilEpochMs = Optional.ofNullable(builder.berserkActiveUntilEpochMs);
        this.activeBuffs = Collections.unmodifiableList(
                Objects.requireNonNull(builder.activeBuffs, "activeBuffs"));
    }

    @Override
    public String getMachineFullName() {
        return machineFullName;
    }

    @Override
    public long getSnapshotEpochMs() {
        return snapshotEpochMs;
    }

    @Override
    public Optional<Integer> getSelfEntityId() {
        return selfEntityId;
    }

    @Override
    public Optional<Integer> getCurrentTargetEntityId() {
        return currentTargetEntityId;
    }

    @Override
    public int getCurrentHp() {
        return currentHp;
    }

    @Override
    public int getMaxHp() {
        return maxHp;
    }

    @Override
    public int getCurrentMp() {
        return currentMp;
    }

    @Override
    public int getMaxMp() {
        return maxMp;
    }

    @Override
    public float getSelfX() {
        return selfX;
    }

    @Override
    public float getSelfY() {
        return selfY;
    }

    @Override
    public float getSelfZ() {
        return selfZ;
    }

    @Override
    public List<MonsterRef> getNearbyMonsters() {
        return nearbyMonsters;
    }

    @Override
    public List<DroppedItemRef> getNearbyLoot() {
        return nearbyLoot;
    }

    @Override
    public Map<Integer, Long> getSkillCooldownReadyAtEpochMs() {
        return skillCooldownReadyAtEpochMs;
    }

    @Override
    public boolean isSkillCastInFlight() {
        return skillCastInFlight;
    }

    @Override
    public Optional<Long> getBerserkActiveUntilEpochMs() {
        return berserkActiveUntilEpochMs;
    }

    @Override
    public List<ActiveBuff> getActiveBuffs() {
        return activeBuffs;
    }

    public static Builder builder(String machineFullName) {
        return new Builder(machineFullName);
    }

    public static final class Builder {
        private final String machineFullName;
        private long snapshotEpochMs = System.currentTimeMillis();
        private Integer selfEntityId;
        private Integer currentTargetEntityId;
        private int currentHp;
        private int maxHp = 1;
        private int currentMp;
        private int maxMp = 1;
        private float selfX;
        private float selfY;
        private float selfZ;
        private List<MonsterRef> nearbyMonsters = Collections.emptyList();
        private List<DroppedItemRef> nearbyLoot = Collections.emptyList();
        private Map<Integer, Long> skillCooldownReadyAtEpochMs = Collections.emptyMap();
        private boolean skillCastInFlight;
        private Long berserkActiveUntilEpochMs;
        private List<ActiveBuff> activeBuffs = Collections.emptyList();

        private Builder(String machineFullName) {
            this.machineFullName = machineFullName;
        }

        public Builder snapshotEpochMs(long snapshotEpochMs) {
            this.snapshotEpochMs = snapshotEpochMs;
            return this;
        }

        public Builder selfEntityId(Integer selfEntityId) {
            this.selfEntityId = selfEntityId;
            return this;
        }

        public Builder currentTargetEntityId(Integer currentTargetEntityId) {
            this.currentTargetEntityId = currentTargetEntityId;
            return this;
        }

        public Builder currentHp(int currentHp) {
            this.currentHp = currentHp;
            return this;
        }

        public Builder maxHp(int maxHp) {
            this.maxHp = maxHp;
            return this;
        }

        public Builder currentMp(int currentMp) {
            this.currentMp = currentMp;
            return this;
        }

        public Builder maxMp(int maxMp) {
            this.maxMp = maxMp;
            return this;
        }

        public Builder selfPosition(float x, float y, float z) {
            this.selfX = x;
            this.selfY = y;
            this.selfZ = z;
            return this;
        }

        public Builder nearbyMonsters(List<MonsterRef> nearbyMonsters) {
            this.nearbyMonsters = nearbyMonsters;
            return this;
        }

        public Builder nearbyLoot(List<DroppedItemRef> nearbyLoot) {
            this.nearbyLoot = nearbyLoot;
            return this;
        }

        public Builder skillCooldownReadyAtEpochMs(Map<Integer, Long> skillCooldownReadyAtEpochMs) {
            this.skillCooldownReadyAtEpochMs = skillCooldownReadyAtEpochMs;
            return this;
        }

        public Builder skillCastInFlight(boolean skillCastInFlight) {
            this.skillCastInFlight = skillCastInFlight;
            return this;
        }

        public Builder berserkActiveUntilEpochMs(Long berserkActiveUntilEpochMs) {
            this.berserkActiveUntilEpochMs = berserkActiveUntilEpochMs;
            return this;
        }

        public Builder activeBuffs(List<ActiveBuff> activeBuffs) {
            this.activeBuffs = activeBuffs;
            return this;
        }

        public CombatSnapshot build() {
            return new CombatSnapshot(this);
        }
    }
}
