package org.sokybot.trade.events;

import java.util.Objects;

import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Base for immutable trade/stall domain events keyed by {@code groupName.machineName}.
 */
public abstract class AbstractTradeGameEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;

    protected AbstractTradeGameEvent(String machineFullName) {
        this.fullName = Objects.requireNonNull(machineFullName, "machineFullName");
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public final String getFullName() {
        return fullName;
    }

    @Override
    public final long getTimestamp() {
        return timestamp;
    }
}
