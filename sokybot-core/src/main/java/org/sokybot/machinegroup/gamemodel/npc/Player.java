package org.sokybot.machinegroup.gamemodel.npc;

import org.sokybot.machinegroup.gamemodel.item.Inventory;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;

@Data
@ToString
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Player extends IFighter {

	@Setter(value = AccessLevel.NONE)
	//@Getter(value = AccessLevel.NONE)
	@Delegate
	private Inventory itemInventory = new Inventory();

	private byte charScale; // shared

	private boolean hasMask;

	private String name;

    private byte level ;

	private JobType jobType = JobType.UNKNOWN;

	private byte jobLvl;

	private PVPState pvpState = PVPState.UNKNOWN;

	private boolean inCombat;

	private boolean hasTransport;

	private int transportId;

	private byte scrollMode;

	private byte pvpFlag;

	private InteractionMode interactionMode = InteractionMode.None;

	private String guildName;
	private int guildId;
	private String guildGrantName;
	private int guildLastCrest;

	private int unionId;
	private int unionLastCrest;

	private boolean guildIsFriendly;
	private byte guildSeigeAuthority;

	private String stallName;
	private int stallRefId;

	private byte equipmentCooldown;

	public Player(NPCEntity entity) {
		super(entity);
	}
	
	

	
	public int getLevel() { 
		return this.level ; 
	}
 
	
	

}
