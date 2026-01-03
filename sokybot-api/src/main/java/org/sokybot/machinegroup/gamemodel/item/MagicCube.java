package org.sokybot.machinegroup.gamemodel.item;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper =  true)
public class MagicCube extends Item {

	private int storedItemCount ; 
	
	public MagicCube(Item item) {
		super(item) ; 
	}
	
	
}
