package org.sokybot.combat.api;

import java.util.Collections;
import java.util.List;

/**
 * Read-only combat settings for strategies and the behavior cycle (mirrors persisted user config).
 */
public interface ICombatSettings {

    boolean isAutoAttack();

    int getHpPotionThresholdPercent();

    int getMpPotionThresholdPercent();

    /** Inventory item ref id for HP pots; {@code 0} lets behaviors auto-detect by name pattern. */
    int getHpPotionItemRefId();

    /** Inventory item ref id for MP pots; {@code 0} lets behaviors auto-detect by name pattern. */
    int getMpPotionItemRefId();

    int getLeashRadius();

    /** Max pickup distance around the character for ground loot. */
    int getLootRadius();

    int getMaxEngageDistance();

    /** Party member entity unique ids for KS sharing rules; may be empty. */
    List<Integer> getPartyMemberEntityIds();

    /**
     * Ordered skill ref ids for the main rotation.
     */
    List<Integer> getAttackSkillRotation();

    List<Integer> getBuffSkillRotation();

    List<Integer> getMobRefIdAllowList();

    List<Integer> getMobRefIdBlockList();

    boolean isPickupGold();

    List<Integer> getLootItemRefIdWhitelist();

    boolean isReturnToTownOnNearDeath();

    /** Ordered imbue skill ref ids; empty to disable imbue automation. */
    default List<Integer> getImbueSkillRotation() {
        return Collections.emptyList();
    }

    /** Refresh imbue when remaining duration is at or below this many milliseconds. */
    default int getImbueRefreshLeadMs() {
        return 1500;
    }

    /**
     * If positive, refresh imbue after this many basic attacks since the last imbue cast; {@code 0} disables the
     * attack-count trigger.
     */
    default int getImbueMaxAttacksBetweenCasts() {
        return 0;
    }

    /**
     * Inventory slot index holding the primary (right-hand) weapon for the main damage loadout; {@code -1} unset.
     */
    default int getMainDamagePrimaryInventorySlot() {
        return -1;
    }

    /**
     * Inventory slot index for secondary (shield / left); {@code -1} unset.
     */
    default int getMainDamageSecondaryInventorySlot() {
        return -1;
    }

    default int getBuffCasterPrimaryInventorySlot() {
        return -1;
    }

    default int getBuffCasterSecondaryInventorySlot() {
        return -1;
    }

    /** Max wait for server ack when swapping weapon sets. */
    default int getWeaponSwapTimeoutMs() {
        return 4000;
    }

    /** Buffs cast while in {@link WeaponLoadout#BUFF_CASTER} (separate from town {@link #getBuffSkillRotation()}). */
    default List<Integer> getBuffCasterSkillRotation() {
        return Collections.emptyList();
    }

    /**
     * Derives an {@link ICombatPolicy} from these settings for strategy calls.
     */
    ICombatPolicy toPolicy();
}
