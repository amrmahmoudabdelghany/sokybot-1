package org.sokybot.combat.projections.internal;

import org.sokybot.gameevents.dto.ItemData;
import org.sokybot.gameevents.enums.Rarity;

/**
 * Approximate client loot-owner countdown by item rarity (additive lookup).
 */
final class LootOwnerDurationTable {

    private static final long DEFAULT_MS = 15_000L;

    private LootOwnerDurationTable() {
    }

    static long durationMsFor(ItemData item) {
        if (item == null) {
            return DEFAULT_MS;
        }
        Rarity r = item.getRarity();
        if (r == null) {
            return DEFAULT_MS;
        }
        switch (r) {
            case Normal:
                return 15_000L;
            case Blue:
                return 30_000L;
            case Sox:
                return 60_000L;
            case UNKNOWN:
            default:
                return DEFAULT_MS;
        }
    }
}
