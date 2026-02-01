package org.sokybot.gamemodel.internal;

import java.io.Serializable;
import org.sokybot.gamemodel.model.IItem;
import org.sokybot.gameevents.dto.ItemData;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Item extends Spawn implements IItem, Serializable {

    // Removed ItemEntity delagation
    // private ItemEntity itemEntity;

    private byte slot;
    private int rentType;

    private short stackCount;
    private byte attributeAssimilationProbability;

    // private ItemRent rent; // ItemRent type? Assuming internal or available
    // class. If missing, I'll comment out.
    // Explicitly defining fields that were delegated or needed

    public Item(ItemData data) {
        super(data);
        this.stackCount = (short) data.getAmount();
        // this.plus = data.getPlus();
    }

    public Item(ItemData data, byte slot) {
        this(data);
        this.slot = slot;
    }

    // Implementing IItem methods (previously delegated)
    // TODO: Connect to Static Data Service to get real values

    public String getLongId() {
        return "ITEM_" + getRefId(); // Placeholder
    }

    public byte getLevel() {
        return 0; // Placeholder
    }

    public boolean isWeapon() {
        return false; // Placeholder
    }

    public boolean isShield() {
        return false; // Placeholder
    }

    public boolean isAccessory() {
        return false; // Placeholder
    }

    public boolean isEquipment() {
        return false; // IItem might have this?
    }

    // Stub for rent if ItemRent is missing or complex
    // @Delegate private ItemRent rent;

    // If IItem has methods derived from ItemRent, they need implementation
    // Assuming straightforward for now.
}
