package org.sokybot.combat.api;

/**
 * Normalized combat tier for mob filtering and KS / avoidance policies (additive).
 */
public enum MonsterTier {
    NORMAL,
    CHAMPION,
    UNIQUE,
    GIANT,
    TITAN,
    ELITE,
    PARTY,
    EVENT,
    QUEST,
    UNKNOWN
}
