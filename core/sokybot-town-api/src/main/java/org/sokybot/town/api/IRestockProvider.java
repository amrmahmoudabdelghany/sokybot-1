package org.sokybot.town.api;

import java.util.Optional;

/**
 * Decides whether a shopping / potion vendor visit is required.
 */
public interface IRestockProvider {

    Optional<RestockOrder> needs(IInventorySnapshot inventory, ITownPolicy policy);
}
