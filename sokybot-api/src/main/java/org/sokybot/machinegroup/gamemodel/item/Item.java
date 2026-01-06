package org.sokybot.machinegroup.gamemodel.item;

import java.io.Serializable;
import org.sokybot.persistence.entities.ItemEntity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import lombok.experimental.Delegate;



@Data
public class Item  implements Serializable{

	@Delegate
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private ItemEntity itemEntity ; 

	private byte slot ; 
	private int rentType ; 
	
	private short stackCount ; 
	private byte attributeAssimilationProbability ; 
	
	@Delegate
	private ItemRent rent  ; 
	
	
	public Item(Item item ) { 
		this.itemEntity = item.itemEntity ; 
		this.slot = item.slot ; 
		this.rentType = item.rentType ; 
		this.stackCount = item.stackCount ; 
		this.attributeAssimilationProbability = item.attributeAssimilationProbability ; 
		this.rent = item.rent ; 
	}
    
    public String getLongId() {
        return itemEntity.getLongId();
    }
    
    public byte getSlot() {
        return this.slot;
    }
    
    public void setSlot(byte slot) {
        this.slot = slot;
    }
	
	public Item(ItemEntity item) { 
		this.itemEntity = item ; 
	}
	
	
}
