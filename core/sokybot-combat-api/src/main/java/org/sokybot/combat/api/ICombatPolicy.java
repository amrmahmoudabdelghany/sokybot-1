package org.sokybot.combat.api;

import java.util.List;

/**
 * Read-only policy thresholds and filters driving strategy decisions.
 */
public interface ICombatPolicy {

    int getHpPotionThresholdPercent();

    int getMpPotionThresholdPercent();

    int getHpPotionItemRefId();

    int getMpPotionItemRefId();

    int getMaxEngageDistance();

    int getLeashRadius();

    /**
     * Minimum monster level relative to player (negative means can attack lower levels).
     */
    int getMinMobLevelDelta();

    int getMaxMobLevelDelta();

    boolean isPreferAggressiveOnSelf();

    boolean isPickupGold();

    List<Integer> getLootItemRefIdWhitelist();

    List<Integer> getMobRefIdAllowList();

    List<Integer> getMobRefIdBlockList();
}
