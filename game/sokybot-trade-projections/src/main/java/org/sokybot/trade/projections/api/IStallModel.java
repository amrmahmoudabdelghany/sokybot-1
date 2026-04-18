package org.sokybot.trade.projections.api;

import java.util.Optional;

/**
 * In-memory projection of stall state per machine (group.machine).
 */
public interface IStallModel {

    Optional<StallSnapshot> getSnapshot(String machineFullName);
}
