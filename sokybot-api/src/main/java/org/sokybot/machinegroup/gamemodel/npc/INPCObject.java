package org.sokybot.machinegroup.gamemodel.npc;

import org.sokybot.machinegroup.gamemodel.ISpawnable;
import org.sokybot.persistence.entities.NPCEntity;

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

	private int HP ; 

	public int getHP() { return HP; }
	public void setHP(int HP) { this.HP = HP; }
	
	public NPCEntity getEntity() { return entity; }

	public INPCObject(NPCEntity entity) { 
		this.entity = entity ; 
	}
	
	public INPCObject() {
	}
	
	
}
