package org.sokybot.machinegroup.gamemodel.npc;


import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper =  true)
public class Monster extends IFighter {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private byte appearance ; 
	
	private int maxHP ; 
	
	@Setter(value = AccessLevel.NONE)
	private MonsterType monsterType = MonsterType.UNKNOWN  ; 

	
	public Monster(NPCEntity entity) {
		super(entity) ; 
		this.maxHP = entity.getHP()  ; 
	}
	
	
	public void setStrengthLevel(byte val) { 
		
		switch(val) { 
		case 0x00 : 
			this.monsterType = MonsterType.Normal ; 
			break ; 
		case 0x01 : 
			this.monsterType = MonsterType.Champion ; 
			this.maxHP *= 2 ; 
			break ; 
		case 0x03 : 
			this.monsterType = MonsterType.Unique ; 
			break ; 
		case 0x04 : 
			this.monsterType = MonsterType.Giant ; 
			this.maxHP *= 20 ; 
			break ; 
		case 0x05 : 
			this.monsterType = MonsterType.Strong ;
			break ; 
		case 0x06 : 
			this.monsterType = MonsterType.Elite1 ; 
			break ; 
		case 0x07 : 
			this.monsterType = MonsterType.Elite2 ; 
			break ;
		case 0x10 :
			this.monsterType = MonsterType.Party ; 
			this.maxHP *= 10 ; 
			break ; 
		case 0x11 :
			this.monsterType = MonsterType.PartyChampion ; 
			this.maxHP *= 20 ; 
			break ; 
		case 0x13 : 
			this.monsterType = MonsterType.PartyUnique ; 
			break ; 
		case 0x14 : 
			this.monsterType = MonsterType.PartyGiant ;
			this.maxHP *= 200 ; 
			break ; 
		case 0x15 : 
			this.monsterType = MonsterType.PartyStrong ; 
			break ;
		case 0x16 :
			this.monsterType = MonsterType.Elite ; 
		
		}
		

	}

}
