package org.sokybot.machine.model.item;

import org.sokybot.game.enums.PetStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PetScroll extends Item {

	
	private PetStatus petStatus ; 
	
	private int uniqueId ; 
	
	
	private String petName ; 
	
	
	private byte unk02 ; 
	
	
	
	public PetScroll(Item item) { 
		super(item) ; 
		
	}
	
	
}
