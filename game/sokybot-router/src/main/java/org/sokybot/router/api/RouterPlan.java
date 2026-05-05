package org.sokybot.router.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable router outcome: designated mule and proposed trades.
 */
public final class RouterPlan {

    private final String muleMachineId;
    private final List<TradeMoveDto> trades;

    public RouterPlan(String muleMachineId, List<TradeMoveDto> trades) {
        Objects.requireNonNull(trades, "trades");
        List<TradeMoveDto> copy = new ArrayList<>();
        for (TradeMoveDto t : trades) {
            if (t != null) {
                copy.add(t);
            }
        }
        this.muleMachineId = muleMachineId;
        this.trades = Collections.unmodifiableList(copy);
    }

    /**
     * Machine id of the solver-designated mule, or {@code null} if none.
     */
    public String getMuleMachineId() {
        return muleMachineId;
    }

    public List<TradeMoveDto> getTrades() {
        return trades;
    }
}
