package org.sokybot.combat.api;

/**
 * Recovery action produced by {@link IRecoveryStrategy}.
 */
public enum RecoveryActionKind {
    HP_POTION,
    MP_POTION,
    /** Pet HP or universal pill-style recovery when distinguished by item ref */
    SECONDARY_CONSUMABLE,
    ITEM_USE,
    RETURN_SCROLL,
    /** Stop combat movement and wait */
    SIT,
    NONE
}
