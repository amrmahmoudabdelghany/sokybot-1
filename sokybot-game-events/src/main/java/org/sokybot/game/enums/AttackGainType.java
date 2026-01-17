package org.sokybot.game.enums;

public enum AttackGainType {

	GOLD((byte)0x01) , 
	SP((byte)0x02) ,
	ZERK((byte)0x04) , 
	UNKNOWN((byte)-1); 
	
	private byte val ; 
	
	private AttackGainType(byte val) {
		 this.val = val ; 
	}
	
	public static AttackGainType of(byte val) { 
		for(AttackGainType t : values()) { 
			if(t.val == val) return t ; 
		}
		return UNKNOWN ; 
	}
}
