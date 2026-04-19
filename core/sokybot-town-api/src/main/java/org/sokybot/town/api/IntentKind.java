package org.sokybot.town.api;

/**
 * Orthogonal lifecycle domains that may preempt one another (combat guard yield, town loop, death handling).
 */
public enum IntentKind {

    /** Default grinding / autonomous combat intent. */
    COMBAT,

    /** Vendor visits, banking, repairs, recalls — town logistics. */
    TOWN,

    /** Recovery after death or disconnect-like failure modes. */
    DEATH,

    /** Agent idle / manual takeover / explicit pause. */
    IDLE
}
