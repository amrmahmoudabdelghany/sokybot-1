package org.sokybot.storage.api;

/**
 * Behavior ids and workflow blackboard keys for storage automation (town-cycle hosted).
 */
public final class StorageCycleKeys {

    private StorageCycleKeys() {
    }

    public static final String BEHAVIOR_DEPOSIT_LOOT = "deposit-loot";

    public static final String BEHAVIOR_WITHDRAW_CONSUMABLE = "withdraw-consumable";

    public static final String KEY_LAST_STORAGE_OPEN_AT_MS = "storage.lastOpenAtEpochMs";

    public static final String KEY_ACTIVE_DEPOSIT_QUEUE = "storage.activeDepositQueue";

    public static final String KEY_ACTIVE_WITHDRAW_QUEUE = "storage.activeWithdrawQueue";

    public static final String KEY_STORAGE_OPEN_IN_PROGRESS = "storage.openInProgress";
}
