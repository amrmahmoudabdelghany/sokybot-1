package org.sokybot.town.api;

import java.util.Optional;

/**
 * Decides whether a blacksmith repair visit is required.
 */
public interface IRepairProvider {

    Optional<RepairOrder> needs(IDurabilitySnapshot durability, ITownPolicy policy);
}
