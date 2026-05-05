package org.sokybot.router.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Planning entity: one item stack that may be reassigned to another bot's inventory.
 */
@PlanningEntity
public class FieldItemEntity {

    @PlanningId
    private String uniqueItemId;

    private int slotsTaken;

    private long goldValue;

    private String originalBotMachineId;

    /** Inventory slot index from {@link org.sokybot.town.api.ItemStackSnapshot#getSlotIndex()}. */
    private int slotIndex;

    /** Stack quantity from inventory snapshot. */
    private int stackQuantity;

    /** Item reference id from inventory snapshot. */
    private int itemRefId;

    @PlanningVariable(valueRangeProviderRefs = "botRange")
    private SwarmBotEntity assignedBot;

    public FieldItemEntity() {
    }

    public FieldItemEntity(
            String uniqueItemId,
            int slotsTaken,
            long goldValue,
            String originalBotMachineId,
            SwarmBotEntity assignedBot) {
        this.uniqueItemId = uniqueItemId;
        this.slotsTaken = slotsTaken;
        this.goldValue = goldValue;
        this.originalBotMachineId = originalBotMachineId;
        this.assignedBot = assignedBot;
    }

    public String getUniqueItemId() {
        return uniqueItemId;
    }

    public void setUniqueItemId(String uniqueItemId) {
        this.uniqueItemId = uniqueItemId;
    }

    public int getSlotsTaken() {
        return slotsTaken;
    }

    public void setSlotsTaken(int slotsTaken) {
        this.slotsTaken = slotsTaken;
    }

    public long getGoldValue() {
        return goldValue;
    }

    public void setGoldValue(long goldValue) {
        this.goldValue = goldValue;
    }

    public String getOriginalBotMachineId() {
        return originalBotMachineId;
    }

    public void setOriginalBotMachineId(String originalBotMachineId) {
        this.originalBotMachineId = originalBotMachineId;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public int getStackQuantity() {
        return stackQuantity;
    }

    public void setStackQuantity(int stackQuantity) {
        this.stackQuantity = stackQuantity;
    }

    public int getItemRefId() {
        return itemRefId;
    }

    public void setItemRefId(int itemRefId) {
        this.itemRefId = itemRefId;
    }

    public SwarmBotEntity getAssignedBot() {
        return assignedBot;
    }

    public void setAssignedBot(SwarmBotEntity assignedBot) {
        this.assignedBot = assignedBot;
    }

    @Override
    public String toString() {
        return uniqueItemId;
    }
}
