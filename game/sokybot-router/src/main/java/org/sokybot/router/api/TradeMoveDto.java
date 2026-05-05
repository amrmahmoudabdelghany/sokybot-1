package org.sokybot.router.api;

import java.util.Objects;

/**
 * One cross-bot item move proposed by the router solver (ACL output).
 */
public final class TradeMoveDto {

    private final String fromMachineId;
    private final String toMachineId;
    private final String uniqueItemId;
    private final int slotIndex;
    private final int stackQuantity;
    private final int itemRefId;

    public TradeMoveDto(
            String fromMachineId,
            String toMachineId,
            String uniqueItemId,
            int slotIndex,
            int stackQuantity,
            int itemRefId) {
        this.fromMachineId = Objects.requireNonNull(fromMachineId, "fromMachineId").trim();
        this.toMachineId = Objects.requireNonNull(toMachineId, "toMachineId").trim();
        this.uniqueItemId = Objects.requireNonNull(uniqueItemId, "uniqueItemId").trim();
        if (this.fromMachineId.isEmpty() || this.toMachineId.isEmpty() || this.uniqueItemId.isEmpty()) {
            throw new IllegalArgumentException("machine ids and uniqueItemId must not be blank");
        }
        this.slotIndex = slotIndex;
        this.stackQuantity = stackQuantity;
        this.itemRefId = itemRefId;
    }

    public String getFromMachineId() {
        return fromMachineId;
    }

    public String getToMachineId() {
        return toMachineId;
    }

    public String getUniqueItemId() {
        return uniqueItemId;
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
