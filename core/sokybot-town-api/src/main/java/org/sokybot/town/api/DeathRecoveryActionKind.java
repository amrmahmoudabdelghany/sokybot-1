package org.sokybot.town.api;

/**
 * High-level death recovery tactics; implementation supplies protocol details out of band.
 */
public enum DeathRecoveryActionKind {

    RESPAWN_AT_TOWN,
    OPEN_RESURRECTION_UI,
    WAIT_FOR_PARTY_REVIVE,
    REQUEST_MANUAL_INTERVENTION,
    CUSTOM
}
