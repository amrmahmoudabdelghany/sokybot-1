package org.sokybot.machine.page.itempage;

import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.machinegroup.gamemodel.setting.ItemAction;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class TableEntry {

	private ItemEntity itemEntity ; 
	private ItemAction action  = ItemAction.IGNORE; 
	
	// Explicit constructor for compilation
	public TableEntry(ItemEntity itemEntity, ItemAction action) {
		this.itemEntity = itemEntity;
		this.action = action;
	}
	
	// Explicit getters for compilation
	public ItemEntity getItemEntity() {
		return itemEntity;
	}
	
	public ItemAction getAction() {
		return action;
	}
}
