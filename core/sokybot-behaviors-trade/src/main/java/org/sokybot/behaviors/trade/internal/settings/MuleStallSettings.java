package org.sokybot.behaviors.trade.internal.settings;

/**
 * Settings shared by all behaviors in {@code stall-cycle}.
 */
public final class MuleStallSettings {

    private int freeCargoSlots = 40;
    private String stallLocationHint = "";
    private String stallTitle = "Bot Stall";

    public MuleStallSettings() {
    }

    public int getFreeCargoSlots() {
        return freeCargoSlots;
    }

    public void setFreeCargoSlots(int freeCargoSlots) {
        if (freeCargoSlots >= 0) {
            this.freeCargoSlots = freeCargoSlots;
        }
    }

    public String getStallLocationHint() {
        return stallLocationHint;
    }

    public void setStallLocationHint(String stallLocationHint) {
        if (stallLocationHint != null) {
            this.stallLocationHint = stallLocationHint;
        }
    }

    public String getStallTitle() {
        return stallTitle;
    }

    public void setStallTitle(String stallTitle) {
        if (stallTitle != null && !stallTitle.trim().isEmpty()) {
            this.stallTitle = stallTitle.trim();
        }
    }
}
