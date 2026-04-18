package org.sokybot.trade.events;

/**
 * Stall destroyed / closed (opcode 0x30B9 pattern).
 */
public final class StallClosed extends AbstractTradeGameEvent {

    private final int stallEntityId;

    public StallClosed(String machineFullName, int stallEntityId) {
        super(machineFullName);
        this.stallEntityId = stallEntityId;
    }

    public int getStallEntityId() {
        return stallEntityId;
    }
}
