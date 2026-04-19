package org.sokybot.combat.api;

/**
 * Logical weapon layout for mid-combat swapping (Euro-style buff weapon vs main damage).
 */
public enum WeaponLoadout {

    /** Primary damage weapon (and optional shield). */
    MAIN_DAMAGE,

    /** Buff / caster weapon set. */
    BUFF_CASTER,

    /** Loadout not yet resolved. */
    UNKNOWN
}
