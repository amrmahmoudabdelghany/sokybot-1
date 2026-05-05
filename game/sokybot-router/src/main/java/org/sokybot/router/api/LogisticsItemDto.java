package org.sokybot.router.api;

import java.util.Objects;

/**
 * Router logistics input: one inventory stack (no Timefold types).
 */
public final class LogisticsItemDto {

    private final String uniqueItemId;
    private final int slotsTaken;
    private final long goldValue;
    private final String originalBotMachineId;
    private final int slotIndex;
    private final int stackQuantity;
    private final int itemRefId;

    public LogisticsItemDto(
            String uniqueItemId,
            int slotsTaken,
            long goldValue,
            String originalBotMachineId,
            int slotIndex,
            int stackQuantity,
            int itemRefId) {
        this.uniqueItemId = Objects.requireNonNull(uniqueItemId, "uniqueItemId").trim();
        if (this.uniqueItemId.isEmpty()) {
            throw new IllegalArgumentException("uniqueItemId must not be blank");
        }
        this.slotsTaken = slotsTaken;
        this.goldValue = goldValue;
        this.originalBotMachineId = Objects.requireNonNull(originalBotMachineId, "originalBotMachineId").trim();
        if (this.originalBotMachineId.isEmpty()) {
            throw new IllegalArgumentException("originalBotMachineId must not be blank");
        }
        this.slotIndex = slotIndex;
        this.stackQuantity = stackQuantity;
        this.itemRefId = itemRefId;
    }

    public String getUniqueItemId() {
        return uniqueItemId;
    }

    public int getSlotsTaken() {
        return slotsTaken;
    }

    public long getGoldValue() {
        return goldValue;
    }

    public String getOriginalBotMachineId() {
        return originalBotMachineId;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public int getStackQuantity() {
        return stackQuantity;
    }

    public int getItemRefId() {
        return itemRefId;
    }
}
