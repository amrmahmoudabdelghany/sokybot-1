package org.sokybot.gamemodel.internal;

import java.io.Serializable;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.gamemodel.model.IItem;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Delegate;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Item extends Spawn implements IItem, Serializable {

	@Delegate
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private ItemEntity itemEntity ; 

	private byte slot ; 
	private int rentType ; 
	
	private short stackCount ; 
	private byte attributeAssimilationProbability ; 
	
	@Delegate
	private ItemRent rent;

	public Item(Item item ) { 
        super(org.sokybot.game.dto.ItemData.builder()
                .uniqueId(item.getUniqueId())
                .refId(item.getRefId())
                .build()); 
		this.itemEntity = item.itemEntity ; 
		this.slot = item.slot ; 
		this.rentType = item.rentType ; 
		this.stackCount = item.stackCount ; 
		this.attributeAssimilationProbability = item.attributeAssimilationProbability ; 
		this.rent = item.rent ; 
	}
	
	public Item(ItemEntity entity, org.sokybot.game.dto.ItemData data) {
        super(data);
		this.itemEntity = entity;
		this.stackCount = (short) data.getAmount();
	}
    
    // IItem/ItemEntity Delegations
    public String getLongId() {
        return itemEntity != null ? itemEntity.getLongId() : null;
    }
    
    public byte getLevel() {
         return itemEntity != null ? (byte) itemEntity.getLevel() : 0;
    }

    public boolean isWeapon() {
        return itemEntity != null && itemEntity.isWeapon();
    }
    
    public boolean isShield() {
         return itemEntity != null && itemEntity.isShield();
    }
    
    public boolean isAccessory() {
         return itemEntity != null && itemEntity.isAccessory();
    }
    
    // Manual setters for fields that aren't auto-generated or needed
	public Item(ItemEntity item) { 
        super(org.sokybot.game.dto.ItemData.builder()
            .refId(item.getRefId())
            .name(item.getName())
            .build());
		this.itemEntity = item ; 
	}
}
