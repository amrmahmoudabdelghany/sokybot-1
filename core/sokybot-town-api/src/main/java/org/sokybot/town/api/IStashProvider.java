package org.sokybot.town.api;

import java.util.Optional;

/**
 * Decides whether banking / guild stash / overflow handling is required.
 */
public interface IStashProvider {

    Optional<StashOrder> needs(IInventorySnapshot inventory, ITownPolicy policy);
}
