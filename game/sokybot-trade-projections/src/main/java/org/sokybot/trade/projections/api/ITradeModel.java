package org.sokybot.trade.projections.api;

import java.util.Optional;

/**
 * In-memory projection of P2P trade window state per machine (group.machine).
 */
public interface ITradeModel {

    Optional<TradeSnapshot> getSnapshot(String machineFullName);
}
