package org.sokybot.combat.api;

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

    int getMaxEngageDistance();

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

    /**
     * Derives an {@link ICombatPolicy} from these settings for strategy calls.
     */
    ICombatPolicy toPolicy();
}
