package org.sokybot.machine.model.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sokybot.game.enums.EquipmentSlot;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Inventory {

	private byte itemInventorySize ; 
	private byte itemCount ; 
	
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private Map<Byte , Item> items = new HashMap<>() ; 
	
	private byte avaterInventorySize ; 
	private byte avaterItemCount ; 
	
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private Map<Byte , Item> avaterItems = new HashMap<>() ; 
	
	
	
	
	
	public boolean hasJopEquipment() { 
	  
		return getItemAt(EquipmentSlot.Extra.getSlot())
				.map((item)->{

					String longId = item.getLongId() ; 
					
					return longId.contains("_TRADE_TRADER_") || longId.contains("_TRADE_HUNTER_") || longId.contains("_TRADE_THIEF_") ;
				
				}).orElse(false);

	}
	
	public void addItem(Item item ) { 
		System.out.println("Item : " + item) ;
		byte itemSlot = item.getSlot() ; 
		
		if(itemSlot > itemInventorySize  ) 
			throw new IllegalArgumentException("ItemInventory  : invalid slot " +
		itemSlot   + " Where size "  + itemInventorySize) ; 
		items.put(itemSlot, item) ; 
	}
	
	public Optional<Item> getItemAt(byte slot) { 
		return Optional.ofNullable(items.get(slot)) ; 
	}
	
	public void removeItemAt(byte slot) { 
		this.items.remove(slot) ; 
	}

	public void addAvaterItem(Item item ) { 
		byte itemSlot = item.getSlot() ; 
		
		if(itemSlot > avaterInventorySize  ) 
			throw new IllegalArgumentException("AvaterInventory  : invalid slot " +
					itemSlot   + " Where size "  + avaterInventorySize) ; 
		avaterItems.put(itemSlot, item) ; 
	}
	
	public Optional<Item> getAvaterItemAt(byte slot) { 
		return Optional.ofNullable(avaterItems.get(slot)) ; 
	}
	
	public void removeAvaterItem(byte slot) { 
		this.avaterItems.remove(slot) ; 
	}

	public void clearItemInventory() { 
		this.items.clear(); 
	}

	public void clearAvaterInventory() { 
		this.avaterItems.clear();  
	}
	
	
}
