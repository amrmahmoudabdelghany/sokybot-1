package org.sokybot.machinegroup.gamemodel.npc;

import org.sokybot.machinegroup.gamemodel.ISpawnable;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class INPCObject extends ISpawnable{

	
	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	@Delegate
	protected NPCEntity entity ; 

	
	//private String name; // shared
	//private byte level; // shared -- Current charLvl
	private int HP ; 

	public INPCObject(NPCEntity entity) { 
		this.entity = entity ; 
	}
	
	public INPCObject() {
	}
	
	
}
