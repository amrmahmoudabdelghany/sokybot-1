package org.sokybot.behaviors.trade.internal.settings;

/**
 * Settings object shared by all behaviors in {@code trade-cycle} (assembler requires one type).
 */
public final class FarmerTradeSettings {

    /** Advertised free cargo expectation when matching mules (hint only for v1). */
    private int expectedMuleFreeSlots = 8;

    public FarmerTradeSettings() {
    }

    public int getExpectedMuleFreeSlots() {
        return expectedMuleFreeSlots;
    }

    public void setExpectedMuleFreeSlots(int expectedMuleFreeSlots) {
        if (expectedMuleFreeSlots >= 0) {
            this.expectedMuleFreeSlots = expectedMuleFreeSlots;
        }
    }
}
