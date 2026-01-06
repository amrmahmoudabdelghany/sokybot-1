package org.sokybot.machinegroup.gamemodel.npc;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.sokybot.persistence.entities.NPCEntity;

@Data
@EqualsAndHashCode(callSuper =  true)
@ToString(callSuper =  true)
public class Pet extends IFighter{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	private String customName ; 
	private String ownerName ; 
	private int ownerId ; 
	
	
	
	public Pet(NPCEntity entity) { 
		super(entity) ; 
	}
	
	
	
}
