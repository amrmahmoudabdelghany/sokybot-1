package org.sokybot.town.api;

/**
 * Stable string keys for workflow {@code persistentData}, optional OSGi filters, and guard coordination.
 */
public final class TownCycleKeys {

    private TownCycleKeys() {
    }

    /** Registered cycle id passed to BehaviorCycleSpec (town loop). */
    public static final String CYCLE_NAME = "town-cycle";

    /** Behavior ids (normalized for behavior-cycle assembler state names). */
    public static final String BEHAVIOR_DEATH_RECOVERY = "death-recovery";
    public static final String BEHAVIOR_RETURN_TO_TOWN = "return-to-town";
    public static final String BEHAVIOR_WALK_TO_VENDOR = "walk-to-vendor";
    public static final String BEHAVIOR_RESTOCK = "restock";
    public static final String BEHAVIOR_REPAIR = "repair";
    public static final String BEHAVIOR_STASH_LOOT = "stash-loot";
    public static final String BEHAVIOR_WALK_TO_HUNT = "walk-to-hunt";
    public static final String BEHAVIOR_HANDOFF = "handoff";

    /** Persistent map key for the logical machine identity used by town behaviors. */
    public static final String MACHINE_FULL_NAME = "town.machineFullName";

    /** Persistent map key for the last resolved {@link IntentKind} (debug / UX). */
    public static final String LAST_INTENT_KIND = "town.lastIntentKind";

    /** Persistent map key for the current {@link ITownSnapshot} epoch (long). */
    public static final String LAST_TOWN_SNAPSHOT_EPOCH_MS = "town.lastSnapshotEpochMs";

    /** State-local key: restock order in flight. */
    public static final String ACTIVE_RESTOCK_ORDER = "town.activeRestockOrder";

    /** State-local key: repair order in flight. */
    public static final String ACTIVE_REPAIR_ORDER = "town.activeRepairOrder";

    /** State-local key: stash order in flight. */
    public static final String ACTIVE_STASH_ORDER = "town.activeStashOrder";

    /** State-local key: death recovery step in flight. */
    public static final String ACTIVE_DEATH_ACTION = "town.activeDeathRecoveryAction";

    /** State-local key: return-to-hunt route step in flight. */
    public static final String ACTIVE_RETURN_ACTION = "town.activeReturnAction";
}
