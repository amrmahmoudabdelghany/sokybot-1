package org.sokybot.machinegroup.gamemodel.item;

import org.sokybot.machinegroup.gamemodel.ISpawnable;

import lombok.AccessLevel;
import lombok.Data ;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.Getter;
import lombok.experimental.Delegate;

@Data
@EqualsAndHashCode(callSuper = true)
public class DropItem extends ISpawnable{

	
	@Delegate
	@Getter(value = AccessLevel.NONE) 
	@Setter(value = AccessLevel.NONE)
	private ItemEntity itemEntity ; 
	
	private int ownerId ; 
	private int amount ; 
	
	
	private byte plus ; 
	
	private Rarity rarity ; 
	
	private String name ; 
	
	
	public DropItem(ItemEntity entity) { 
		this.itemEntity = entity ; 
	}
	
	
}
