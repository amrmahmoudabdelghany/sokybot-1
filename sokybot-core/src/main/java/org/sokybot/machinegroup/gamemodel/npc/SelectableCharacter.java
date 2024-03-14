
package org.sokybot.machinegroup.gamemodel.npc ; 

import java.util.List;

import org.sokybot.machinegroup.gamemodel.item.Inventory;
import org.sokybot.machinegroup.gamemodel.item.ItemEntity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;


/**
 *
 * @author AMROO
 */
@Data
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class SelectableCharacter extends INPCObject {

	private byte isDeleting;
	private int DeleteTime;
	private GUILD_MEMBER_CLASS GMC = GUILD_MEMBER_CLASS.UNKNOWN;
	private byte isGuildRenameRequired;
	private String currentGuildName;
	private ACADEMY_MEMBER_CLASS AMC = ACADEMY_MEMBER_CLASS.UNKNOWN;
	
	
	private long charEXPOffset; // shared
	private short charSTR; // shared
	private short charINT; // shared
	private short charStatPoint; // shared
	private int charHP; // shared
	private int charMP; // shared
	private int charId ; 
	private String name ;
	
	private int level  ; 
	
	
	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	@Delegate
	private Inventory itemInventory = new Inventory();


	private byte charScale; // shared

	
	
	


}
