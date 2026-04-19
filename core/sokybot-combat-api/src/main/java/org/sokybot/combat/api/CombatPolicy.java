package org.sokybot.combat.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable default implementation of {@link ICombatPolicy}.
 */
public final class CombatPolicy implements ICombatPolicy {

    private final int hpPotionThresholdPercent;
    private final int mpPotionThresholdPercent;
    private final int hpPotionItemRefId;
    private final int mpPotionItemRefId;
    private final int maxEngageDistance;
    private final int leashRadius;
    private final int minMobLevelDelta;
    private final int maxMobLevelDelta;
    private final boolean preferAggressiveOnSelf;
    private final boolean pickupGold;
    private final List<Integer> lootItemRefIdWhitelist;
    private final List<Integer> mobRefIdAllowList;
    private final List<Integer> mobRefIdBlockList;

    private CombatPolicy(Builder builder) {
        this.hpPotionThresholdPercent = builder.hpPotionThresholdPercent;
        this.mpPotionThresholdPercent = builder.mpPotionThresholdPercent;
        this.hpPotionItemRefId = builder.hpPotionItemRefId;
        this.mpPotionItemRefId = builder.mpPotionItemRefId;
        this.maxEngageDistance = builder.maxEngageDistance;
        this.leashRadius = builder.leashRadius;
        this.minMobLevelDelta = builder.minMobLevelDelta;
        this.maxMobLevelDelta = builder.maxMobLevelDelta;
        this.preferAggressiveOnSelf = builder.preferAggressiveOnSelf;
        this.pickupGold = builder.pickupGold;
        this.lootItemRefIdWhitelist = Collections.unmodifiableList(new ArrayList<>(builder.lootItemRefIdWhitelist));
        this.mobRefIdAllowList = Collections.unmodifiableList(new ArrayList<>(builder.mobRefIdAllowList));
        this.mobRefIdBlockList = Collections.unmodifiableList(new ArrayList<>(builder.mobRefIdBlockList));
    }

    @Override
    public int getHpPotionThresholdPercent() {
        return hpPotionThresholdPercent;
    }

    @Override
    public int getMpPotionThresholdPercent() {
        return mpPotionThresholdPercent;
    }

    @Override
    public int getHpPotionItemRefId() {
        return hpPotionItemRefId;
    }

    @Override
    public int getMpPotionItemRefId() {
        return mpPotionItemRefId;
    }

    @Override
    public int getMaxEngageDistance() {
        return maxEngageDistance;
    }

    @Override
    public int getLeashRadius() {
        return leashRadius;
    }

    @Override
    public int getMinMobLevelDelta() {
        return minMobLevelDelta;
    }

    @Override
    public int getMaxMobLevelDelta() {
        return maxMobLevelDelta;
    }

    @Override
    public boolean isPreferAggressiveOnSelf() {
        return preferAggressiveOnSelf;
    }

    @Override
    public boolean isPickupGold() {
        return pickupGold;
    }

    @Override
    public List<Integer> getLootItemRefIdWhitelist() {
        return lootItemRefIdWhitelist;
    }

    @Override
    public List<Integer> getMobRefIdAllowList() {
        return mobRefIdAllowList;
    }

    @Override
    public List<Integer> getMobRefIdBlockList() {
        return mobRefIdBlockList;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int hpPotionThresholdPercent = 50;
        private int mpPotionThresholdPercent = 50;
        private int hpPotionItemRefId;
        private int mpPotionItemRefId;
        private int maxEngageDistance = 80;
        private int leashRadius = 150;
        private int minMobLevelDelta = -5;
        private int maxMobLevelDelta = 5;
        private boolean preferAggressiveOnSelf = true;
        private boolean pickupGold = true;
        private List<Integer> lootItemRefIdWhitelist = new ArrayList<>();
        private List<Integer> mobRefIdAllowList = new ArrayList<>();
        private List<Integer> mobRefIdBlockList = new ArrayList<>();

        public Builder hpPotionThresholdPercent(int hpPotionThresholdPercent) {
            this.hpPotionThresholdPercent = hpPotionThresholdPercent;
            return this;
        }

        public Builder mpPotionThresholdPercent(int mpPotionThresholdPercent) {
            this.mpPotionThresholdPercent = mpPotionThresholdPercent;
            return this;
        }

        public Builder hpPotionItemRefId(int hpPotionItemRefId) {
            this.hpPotionItemRefId = hpPotionItemRefId;
            return this;
        }

        public Builder mpPotionItemRefId(int mpPotionItemRefId) {
            this.mpPotionItemRefId = mpPotionItemRefId;
            return this;
        }

        public Builder maxEngageDistance(int maxEngageDistance) {
            this.maxEngageDistance = maxEngageDistance;
            return this;
        }

        public Builder leashRadius(int leashRadius) {
            this.leashRadius = leashRadius;
            return this;
        }

        public Builder minMobLevelDelta(int minMobLevelDelta) {
            this.minMobLevelDelta = minMobLevelDelta;
            return this;
        }

        public Builder maxMobLevelDelta(int maxMobLevelDelta) {
            this.maxMobLevelDelta = maxMobLevelDelta;
            return this;
        }

        public Builder preferAggressiveOnSelf(boolean preferAggressiveOnSelf) {
            this.preferAggressiveOnSelf = preferAggressiveOnSelf;
            return this;
        }

        public Builder pickupGold(boolean pickupGold) {
            this.pickupGold = pickupGold;
            return this;
        }

        public Builder lootItemRefIdWhitelist(List<Integer> lootItemRefIdWhitelist) {
            this.lootItemRefIdWhitelist = Objects.requireNonNull(lootItemRefIdWhitelist);
            return this;
        }

        public Builder mobRefIdAllowList(List<Integer> mobRefIdAllowList) {
            this.mobRefIdAllowList = Objects.requireNonNull(mobRefIdAllowList);
            return this;
        }

        public Builder mobRefIdBlockList(List<Integer> mobRefIdBlockList) {
            this.mobRefIdBlockList = Objects.requireNonNull(mobRefIdBlockList);
            return this;
        }

        public CombatPolicy build() {
            return new CombatPolicy(this);
        }
    }
}
