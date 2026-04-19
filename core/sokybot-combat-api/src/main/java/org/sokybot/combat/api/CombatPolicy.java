package org.sokybot.combat.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    private final int lootRadius;
    private final List<Integer> partyMemberEntityIds;
    private final int minMobLevelDelta;
    private final int maxMobLevelDelta;
    private final boolean preferAggressiveOnSelf;
    private final boolean pickupGold;
    private final List<Integer> lootItemRefIdWhitelist;
    private final List<Integer> mobRefIdAllowList;
    private final List<Integer> mobRefIdBlockList;
    private final List<Integer> imbueSkillRotation;
    private final int imbueRefreshLeadMs;
    private final int imbueMaxAttacksBetweenCasts;
    private final int mainDamagePrimaryInventorySlot;
    private final int mainDamageSecondaryInventorySlot;
    private final int buffCasterPrimaryInventorySlot;
    private final int buffCasterSecondaryInventorySlot;
    private final int weaponSwapTimeoutMs;
    private final List<Integer> buffCasterSkillRotation;
    private final boolean avoidGiants;
    private final boolean avoidUniques;
    private final boolean avoidChampions;
    private final boolean avoidTitans;
    private final boolean avoidPartyMobs;
    private final boolean avoidQuestMobs;
    private final int recoveryCooldownMs;
    private final int petHungerThresholdPercent;
    private final int petFoodItemRefId;
    private final Set<Integer> ammoConsumingSkillRefIds;
    private final boolean pauseOnEmptyAmmo;

    private CombatPolicy(Builder builder) {
        this.hpPotionThresholdPercent = builder.hpPotionThresholdPercent;
        this.mpPotionThresholdPercent = builder.mpPotionThresholdPercent;
        this.hpPotionItemRefId = builder.hpPotionItemRefId;
        this.mpPotionItemRefId = builder.mpPotionItemRefId;
        this.maxEngageDistance = builder.maxEngageDistance;
        this.leashRadius = builder.leashRadius;
        this.lootRadius = builder.lootRadius;
        this.partyMemberEntityIds = Collections.unmodifiableList(new ArrayList<>(builder.partyMemberEntityIds));
        this.minMobLevelDelta = builder.minMobLevelDelta;
        this.maxMobLevelDelta = builder.maxMobLevelDelta;
        this.preferAggressiveOnSelf = builder.preferAggressiveOnSelf;
        this.pickupGold = builder.pickupGold;
        this.lootItemRefIdWhitelist = Collections.unmodifiableList(new ArrayList<>(builder.lootItemRefIdWhitelist));
        this.mobRefIdAllowList = Collections.unmodifiableList(new ArrayList<>(builder.mobRefIdAllowList));
        this.mobRefIdBlockList = Collections.unmodifiableList(new ArrayList<>(builder.mobRefIdBlockList));
        this.imbueSkillRotation = Collections.unmodifiableList(new ArrayList<>(builder.imbueSkillRotation));
        this.imbueRefreshLeadMs = builder.imbueRefreshLeadMs;
        this.imbueMaxAttacksBetweenCasts = builder.imbueMaxAttacksBetweenCasts;
        this.mainDamagePrimaryInventorySlot = builder.mainDamagePrimaryInventorySlot;
        this.mainDamageSecondaryInventorySlot = builder.mainDamageSecondaryInventorySlot;
        this.buffCasterPrimaryInventorySlot = builder.buffCasterPrimaryInventorySlot;
        this.buffCasterSecondaryInventorySlot = builder.buffCasterSecondaryInventorySlot;
        this.weaponSwapTimeoutMs = builder.weaponSwapTimeoutMs;
        this.buffCasterSkillRotation = Collections.unmodifiableList(new ArrayList<>(builder.buffCasterSkillRotation));
        this.avoidGiants = builder.avoidGiants;
        this.avoidUniques = builder.avoidUniques;
        this.avoidChampions = builder.avoidChampions;
        this.avoidTitans = builder.avoidTitans;
        this.avoidPartyMobs = builder.avoidPartyMobs;
        this.avoidQuestMobs = builder.avoidQuestMobs;
        this.recoveryCooldownMs = builder.recoveryCooldownMs;
        this.petHungerThresholdPercent = builder.petHungerThresholdPercent;
        this.petFoodItemRefId = builder.petFoodItemRefId;
        this.ammoConsumingSkillRefIds = Collections
                .unmodifiableSet(new LinkedHashSet<>(builder.ammoConsumingSkillRefIds));
        this.pauseOnEmptyAmmo = builder.pauseOnEmptyAmmo;
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
    public int getLootRadius() {
        return lootRadius;
    }

    @Override
    public List<Integer> getPartyMemberEntityIds() {
        return partyMemberEntityIds;
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

    @Override
    public List<Integer> getImbueSkillRotation() {
        return imbueSkillRotation;
    }

    @Override
    public int getImbueRefreshLeadMs() {
        return imbueRefreshLeadMs;
    }

    @Override
    public int getImbueMaxAttacksBetweenCasts() {
        return imbueMaxAttacksBetweenCasts;
    }

    @Override
    public int getMainDamagePrimaryInventorySlot() {
        return mainDamagePrimaryInventorySlot;
    }

    @Override
    public int getMainDamageSecondaryInventorySlot() {
        return mainDamageSecondaryInventorySlot;
    }

    @Override
    public int getBuffCasterPrimaryInventorySlot() {
        return buffCasterPrimaryInventorySlot;
    }

    @Override
    public int getBuffCasterSecondaryInventorySlot() {
        return buffCasterSecondaryInventorySlot;
    }

    @Override
    public int getWeaponSwapTimeoutMs() {
        return weaponSwapTimeoutMs;
    }

    @Override
    public List<Integer> getBuffCasterSkillRotation() {
        return buffCasterSkillRotation;
    }

    @Override
    public boolean isAvoidGiants() {
        return avoidGiants;
    }

    @Override
    public boolean isAvoidUniques() {
        return avoidUniques;
    }

    @Override
    public boolean isAvoidChampions() {
        return avoidChampions;
    }

    @Override
    public boolean isAvoidTitans() {
        return avoidTitans;
    }

    @Override
    public boolean isAvoidPartyMobs() {
        return avoidPartyMobs;
    }

    @Override
    public boolean isAvoidQuestMobs() {
        return avoidQuestMobs;
    }

    @Override
    public int getRecoveryCooldownMs() {
        return recoveryCooldownMs;
    }

    @Override
    public int getPetHungerThresholdPercent() {
        return petHungerThresholdPercent;
    }

    @Override
    public int getPetFoodItemRefId() {
        return petFoodItemRefId;
    }

    @Override
    public Set<Integer> getAmmoConsumingSkillRefIds() {
        return ammoConsumingSkillRefIds;
    }

    @Override
    public boolean isPauseOnEmptyAmmo() {
        return pauseOnEmptyAmmo;
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
        private int lootRadius = 40;
        private List<Integer> partyMemberEntityIds = new ArrayList<>();
        private int minMobLevelDelta = -5;
        private int maxMobLevelDelta = 5;
        private boolean preferAggressiveOnSelf = true;
        private boolean pickupGold = true;
        private List<Integer> lootItemRefIdWhitelist = new ArrayList<>();
        private List<Integer> mobRefIdAllowList = new ArrayList<>();
        private List<Integer> mobRefIdBlockList = new ArrayList<>();
        private List<Integer> imbueSkillRotation = new ArrayList<>();
        private int imbueRefreshLeadMs = 1500;
        private int imbueMaxAttacksBetweenCasts;
        private int mainDamagePrimaryInventorySlot = -1;
        private int mainDamageSecondaryInventorySlot = -1;
        private int buffCasterPrimaryInventorySlot = -1;
        private int buffCasterSecondaryInventorySlot = -1;
        private int weaponSwapTimeoutMs = 4000;
        private List<Integer> buffCasterSkillRotation = new ArrayList<>();
        private boolean avoidGiants = true;
        private boolean avoidUniques = true;
        private boolean avoidChampions = true;
        private boolean avoidTitans = true;
        private boolean avoidPartyMobs = true;
        private boolean avoidQuestMobs = true;
        private int recoveryCooldownMs = 15000;
        private int petHungerThresholdPercent = 20;
        private int petFoodItemRefId = -1;
        private LinkedHashSet<Integer> ammoConsumingSkillRefIds = new LinkedHashSet<>();
        private boolean pauseOnEmptyAmmo = true;

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

        public Builder lootRadius(int lootRadius) {
            this.lootRadius = lootRadius;
            return this;
        }

        public Builder partyMemberEntityIds(List<Integer> partyMemberEntityIds) {
            this.partyMemberEntityIds = Objects.requireNonNull(partyMemberEntityIds);
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

        public Builder imbueSkillRotation(List<Integer> imbueSkillRotation) {
            this.imbueSkillRotation = Objects.requireNonNull(imbueSkillRotation);
            return this;
        }

        public Builder imbueRefreshLeadMs(int imbueRefreshLeadMs) {
            this.imbueRefreshLeadMs = imbueRefreshLeadMs;
            return this;
        }

        public Builder imbueMaxAttacksBetweenCasts(int imbueMaxAttacksBetweenCasts) {
            this.imbueMaxAttacksBetweenCasts = imbueMaxAttacksBetweenCasts;
            return this;
        }

        public Builder mainDamagePrimaryInventorySlot(int mainDamagePrimaryInventorySlot) {
            this.mainDamagePrimaryInventorySlot = mainDamagePrimaryInventorySlot;
            return this;
        }

        public Builder mainDamageSecondaryInventorySlot(int mainDamageSecondaryInventorySlot) {
            this.mainDamageSecondaryInventorySlot = mainDamageSecondaryInventorySlot;
            return this;
        }

        public Builder buffCasterPrimaryInventorySlot(int buffCasterPrimaryInventorySlot) {
            this.buffCasterPrimaryInventorySlot = buffCasterPrimaryInventorySlot;
            return this;
        }

        public Builder buffCasterSecondaryInventorySlot(int buffCasterSecondaryInventorySlot) {
            this.buffCasterSecondaryInventorySlot = buffCasterSecondaryInventorySlot;
            return this;
        }

        public Builder weaponSwapTimeoutMs(int weaponSwapTimeoutMs) {
            this.weaponSwapTimeoutMs = weaponSwapTimeoutMs;
            return this;
        }

        public Builder buffCasterSkillRotation(List<Integer> buffCasterSkillRotation) {
            this.buffCasterSkillRotation = Objects.requireNonNull(buffCasterSkillRotation);
            return this;
        }

        public Builder avoidGiants(boolean avoidGiants) {
            this.avoidGiants = avoidGiants;
            return this;
        }

        public Builder avoidUniques(boolean avoidUniques) {
            this.avoidUniques = avoidUniques;
            return this;
        }

        public Builder avoidChampions(boolean avoidChampions) {
            this.avoidChampions = avoidChampions;
            return this;
        }

        public Builder avoidTitans(boolean avoidTitans) {
            this.avoidTitans = avoidTitans;
            return this;
        }

        public Builder avoidPartyMobs(boolean avoidPartyMobs) {
            this.avoidPartyMobs = avoidPartyMobs;
            return this;
        }

        public Builder avoidQuestMobs(boolean avoidQuestMobs) {
            this.avoidQuestMobs = avoidQuestMobs;
            return this;
        }

        public Builder recoveryCooldownMs(int recoveryCooldownMs) {
            this.recoveryCooldownMs = recoveryCooldownMs;
            return this;
        }

        public Builder petHungerThresholdPercent(int petHungerThresholdPercent) {
            this.petHungerThresholdPercent = petHungerThresholdPercent;
            return this;
        }

        public Builder petFoodItemRefId(int petFoodItemRefId) {
            this.petFoodItemRefId = petFoodItemRefId;
            return this;
        }

        public Builder ammoConsumingSkillRefIds(Set<Integer> ammoConsumingSkillRefIds) {
            this.ammoConsumingSkillRefIds = new LinkedHashSet<>(Objects.requireNonNull(ammoConsumingSkillRefIds));
            return this;
        }

        public Builder pauseOnEmptyAmmo(boolean pauseOnEmptyAmmo) {
            this.pauseOnEmptyAmmo = pauseOnEmptyAmmo;
            return this;
        }

        public CombatPolicy build() {
            return new CombatPolicy(this);
        }
    }
}
