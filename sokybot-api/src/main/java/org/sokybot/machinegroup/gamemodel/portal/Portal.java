package org.sokybot.machinegroup.gamemodel.portal;

import org.sokybot.machinegroup.gamemodel.ISpawnable;
import org.sokybot.persistence.entities.PortalEntity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Data
@EqualsAndHashCode(callSuper = true)
public class Portal extends ISpawnable {

	

	@Delegate
	@Getter(value = AccessLevel.NONE) 
	@Setter(value = AccessLevel.NONE)
	private PortalEntity entity ; 
	
	
	private byte unk0 ; 
	private byte unk1 ; 
	private byte unk2 ; 
	private byte unk3 ; 
	
	
	private int unkInt0 ; 
	private int unkInt1 ; 
	
	private String ownerName ; 
	private int ownerId ; 
	
	private int unkInt2 ; 
	private byte unk4 ; 
	
	
	public Portal(PortalEntity entity) { 
		this.entity = entity ; 
	}
	
}
