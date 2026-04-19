package org.sokybot.town.api;

import java.util.Optional;

import org.sokybot.storage.api.IStorageSnapshot;

/**
 * Chooses the next withdraw move from personal storage toward configured inventory targets.
 */
public interface IWithdrawProvider {

    /** Returns the next planned withdraw (one storage slot) or empty when policy targets are met. */
    Optional<WithdrawOrder> next(IInventorySnapshot inventory, IStorageSnapshot personalStorage,
            ITownPolicy policy);
}
