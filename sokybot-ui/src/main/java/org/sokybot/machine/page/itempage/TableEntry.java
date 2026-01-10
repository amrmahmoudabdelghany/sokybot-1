package org.sokybot.machine.page.itempage;

import org.sokybot.machinegroup.gamemodel.item.ItemEntity;
import org.sokybot.settings.ItemAction;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableEntry {

	private ItemEntity itemEntity ; 
	private ItemAction action  = ItemAction.IGNORE; 
	
}
