package org.sokybot.combat.api;

import java.util.Collections;
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
     * Max ground distance to a drop to attempt pickup, relative to the character (independent of leash anchor).
     */
    int getLootRadius();

    /**
     * Other player entity unique ids (e.g. party) allowed to share a mob for KS rules; may be empty.
     */
    List<Integer> getPartyMemberEntityIds();

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

    default List<Integer> getImbueSkillRotation() {
        return Collections.emptyList();
    }

    default int getImbueRefreshLeadMs() {
        return 1500;
    }

    default int getImbueMaxAttacksBetweenCasts() {
        return 0;
    }

    default int getMainDamagePrimaryInventorySlot() {
        return -1;
    }

    default int getMainDamageSecondaryInventorySlot() {
        return -1;
    }

    default int getBuffCasterPrimaryInventorySlot() {
        return -1;
    }

    default int getBuffCasterSecondaryInventorySlot() {
        return -1;
    }

    default int getWeaponSwapTimeoutMs() {
        return 4000;
    }

    default List<Integer> getBuffCasterSkillRotation() {
        return Collections.emptyList();
    }
}
