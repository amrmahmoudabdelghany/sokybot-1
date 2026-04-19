package org.sokybot.behaviors.combat.internal.settings;

import java.util.ArrayList;
import java.util.List;

import org.sokybot.combat.api.CombatPolicy;
import org.sokybot.combat.api.ICombatPolicy;
import org.sokybot.combat.api.ICombatSettings;

/**
 * Machine-scoped combat settings (scope {@code combat}).
 */
public final class CombatSettings implements ICombatSettings {

    private boolean autoAttack = true;
    private int hpPotionThresholdPercent = 50;
    private int mpPotionThresholdPercent = 50;
    private int hpPotionItemRefId;
    private int mpPotionItemRefId;
    private int leashRadius = 150;
    private int lootRadius = 40;
    private int maxEngageDistance = 80;
    private final List<Integer> attackSkillRotation = new ArrayList<>();
    private final List<Integer> buffSkillRotation = new ArrayList<>();
    private final List<Integer> mobRefIdAllowList = new ArrayList<>();
    private final List<Integer> mobRefIdBlockList = new ArrayList<>();
    private boolean pickupGold = true;
    private final List<Integer> lootItemRefIdWhitelist = new ArrayList<>();
    private final List<Integer> partyMemberEntityIds = new ArrayList<>();
    private boolean returnToTownOnNearDeath;
    private final List<Integer> imbueSkillRotation = new ArrayList<>();
    private int imbueRefreshLeadMs = 1500;
    private int imbueMaxAttacksBetweenCasts;
    private int mainDamagePrimaryInventorySlot = -1;
    private int mainDamageSecondaryInventorySlot = -1;
    private int buffCasterPrimaryInventorySlot = -1;
    private int buffCasterSecondaryInventorySlot = -1;
    private int weaponSwapTimeoutMs = 4000;
    private final List<Integer> buffCasterSkillRotation = new ArrayList<>();

    @Override
    public boolean isAutoAttack() {
        return autoAttack;
    }

    public void setAutoAttack(boolean autoAttack) {
        this.autoAttack = autoAttack;
    }

    @Override
    public int getHpPotionThresholdPercent() {
        return hpPotionThresholdPercent;
    }

    public void setHpPotionThresholdPercent(int hpPotionThresholdPercent) {
        this.hpPotionThresholdPercent = hpPotionThresholdPercent;
    }

    @Override
    public int getMpPotionThresholdPercent() {
        return mpPotionThresholdPercent;
    }

    public void setMpPotionThresholdPercent(int mpPotionThresholdPercent) {
        this.mpPotionThresholdPercent = mpPotionThresholdPercent;
    }

    @Override
    public int getHpPotionItemRefId() {
        return hpPotionItemRefId;
    }

    public void setHpPotionItemRefId(int hpPotionItemRefId) {
        this.hpPotionItemRefId = hpPotionItemRefId;
    }

    @Override
    public int getMpPotionItemRefId() {
        return mpPotionItemRefId;
    }

    public void setMpPotionItemRefId(int mpPotionItemRefId) {
        this.mpPotionItemRefId = mpPotionItemRefId;
    }

    @Override
    public int getLeashRadius() {
        return leashRadius;
    }

    public void setLeashRadius(int leashRadius) {
        this.leashRadius = leashRadius;
    }

    @Override
    public int getLootRadius() {
        return lootRadius;
    }

    public void setLootRadius(int lootRadius) {
        this.lootRadius = lootRadius;
    }

    @Override
    public int getMaxEngageDistance() {
        return maxEngageDistance;
    }

    public void setMaxEngageDistance(int maxEngageDistance) {
        this.maxEngageDistance = maxEngageDistance;
    }

    @Override
    public List<Integer> getPartyMemberEntityIds() {
        return List.copyOf(partyMemberEntityIds);
    }

    @Override
    public List<Integer> getAttackSkillRotation() {
        return List.copyOf(attackSkillRotation);
    }

    @Override
    public List<Integer> getBuffSkillRotation() {
        return List.copyOf(buffSkillRotation);
    }

    @Override
    public List<Integer> getMobRefIdAllowList() {
        return List.copyOf(mobRefIdAllowList);
    }

    @Override
    public List<Integer> getMobRefIdBlockList() {
        return List.copyOf(mobRefIdBlockList);
    }

    @Override
    public boolean isPickupGold() {
        return pickupGold;
    }

    public void setPickupGold(boolean pickupGold) {
        this.pickupGold = pickupGold;
    }

    @Override
    public List<Integer> getLootItemRefIdWhitelist() {
        return List.copyOf(lootItemRefIdWhitelist);
    }

    @Override
    public boolean isReturnToTownOnNearDeath() {
        return returnToTownOnNearDeath;
    }

    public void setReturnToTownOnNearDeath(boolean returnToTownOnNearDeath) {
        this.returnToTownOnNearDeath = returnToTownOnNearDeath;
    }

    @Override
    public List<Integer> getImbueSkillRotation() {
        return List.copyOf(imbueSkillRotation);
    }

    @Override
    public int getImbueRefreshLeadMs() {
        return imbueRefreshLeadMs;
    }

    public void setImbueRefreshLeadMs(int imbueRefreshLeadMs) {
        this.imbueRefreshLeadMs = imbueRefreshLeadMs;
    }

    @Override
    public int getImbueMaxAttacksBetweenCasts() {
        return imbueMaxAttacksBetweenCasts;
    }

    public void setImbueMaxAttacksBetweenCasts(int imbueMaxAttacksBetweenCasts) {
        this.imbueMaxAttacksBetweenCasts = imbueMaxAttacksBetweenCasts;
    }

    @Override
    public int getMainDamagePrimaryInventorySlot() {
        return mainDamagePrimaryInventorySlot;
    }

    public void setMainDamagePrimaryInventorySlot(int mainDamagePrimaryInventorySlot) {
        this.mainDamagePrimaryInventorySlot = mainDamagePrimaryInventorySlot;
    }

    @Override
    public int getMainDamageSecondaryInventorySlot() {
        return mainDamageSecondaryInventorySlot;
    }

    public void setMainDamageSecondaryInventorySlot(int mainDamageSecondaryInventorySlot) {
        this.mainDamageSecondaryInventorySlot = mainDamageSecondaryInventorySlot;
    }

    @Override
    public int getBuffCasterPrimaryInventorySlot() {
        return buffCasterPrimaryInventorySlot;
    }

    public void setBuffCasterPrimaryInventorySlot(int buffCasterPrimaryInventorySlot) {
        this.buffCasterPrimaryInventorySlot = buffCasterPrimaryInventorySlot;
    }

    @Override
    public int getBuffCasterSecondaryInventorySlot() {
        return buffCasterSecondaryInventorySlot;
    }

    public void setBuffCasterSecondaryInventorySlot(int buffCasterSecondaryInventorySlot) {
        this.buffCasterSecondaryInventorySlot = buffCasterSecondaryInventorySlot;
    }

    @Override
    public int getWeaponSwapTimeoutMs() {
        return weaponSwapTimeoutMs;
    }

    public void setWeaponSwapTimeoutMs(int weaponSwapTimeoutMs) {
        this.weaponSwapTimeoutMs = weaponSwapTimeoutMs;
    }

    @Override
    public List<Integer> getBuffCasterSkillRotation() {
        return List.copyOf(buffCasterSkillRotation);
    }

    @Override
    public ICombatPolicy toPolicy() {
        return CombatPolicy.builder()
                .hpPotionThresholdPercent(hpPotionThresholdPercent)
                .mpPotionThresholdPercent(mpPotionThresholdPercent)
                .hpPotionItemRefId(hpPotionItemRefId)
                .mpPotionItemRefId(mpPotionItemRefId)
                .maxEngageDistance(maxEngageDistance)
                .leashRadius(leashRadius)
                .lootRadius(lootRadius)
                .partyMemberEntityIds(new ArrayList<>(partyMemberEntityIds))
                .minMobLevelDelta(-99)
                .maxMobLevelDelta(99)
                .preferAggressiveOnSelf(true)
                .pickupGold(pickupGold)
                .lootItemRefIdWhitelist(new ArrayList<>(lootItemRefIdWhitelist))
                .mobRefIdAllowList(new ArrayList<>(mobRefIdAllowList))
                .mobRefIdBlockList(new ArrayList<>(mobRefIdBlockList))
                .imbueSkillRotation(new ArrayList<>(imbueSkillRotation))
                .imbueRefreshLeadMs(imbueRefreshLeadMs)
                .imbueMaxAttacksBetweenCasts(imbueMaxAttacksBetweenCasts)
                .mainDamagePrimaryInventorySlot(mainDamagePrimaryInventorySlot)
                .mainDamageSecondaryInventorySlot(mainDamageSecondaryInventorySlot)
                .buffCasterPrimaryInventorySlot(buffCasterPrimaryInventorySlot)
                .buffCasterSecondaryInventorySlot(buffCasterSecondaryInventorySlot)
                .weaponSwapTimeoutMs(weaponSwapTimeoutMs)
                .buffCasterSkillRotation(new ArrayList<>(buffCasterSkillRotation))
                .build();
    }
}
