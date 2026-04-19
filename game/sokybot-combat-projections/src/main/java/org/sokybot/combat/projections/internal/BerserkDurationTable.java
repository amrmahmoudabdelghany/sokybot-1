package org.sokybot.combat.projections.internal;

/**
 * vSRO berserk duration by level (additive lookup; used when projecting Berserk window).
 */
final class BerserkDurationTable {

    private static final long DEFAULT_MS = 30_000L;

    private BerserkDurationTable() {
    }

    /**
     * Lv1=30s, Lv2=33s, Lv3=36s, Lv4=39s, Lv5=42s; unknown levels default to 30s.
     */
    static long durationMsForLevel(byte berserkLevel) {
        if (berserkLevel <= 0) {
            return DEFAULT_MS;
        }
        if (berserkLevel <= 5) {
            return 30_000L + (berserkLevel - 1) * 3000L;
        }
        return DEFAULT_MS;
    }
}
