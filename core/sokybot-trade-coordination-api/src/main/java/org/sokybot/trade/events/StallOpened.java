package org.sokybot.trade.events;

/**
 * Stall entity created / opened (opcode 0x30B8 pattern).
 */
public final class StallOpened extends AbstractTradeGameEvent {

    private final int stallEntityId;
    private final String stallTitle;

    public StallOpened(String machineFullName, int stallEntityId, String stallTitle) {
        super(machineFullName);
        this.stallEntityId = stallEntityId;
        this.stallTitle = stallTitle != null ? stallTitle : "";
    }

    public int getStallEntityId() {
        return stallEntityId;
    }

    public String getStallTitle() {
        return stallTitle;
    }
}
