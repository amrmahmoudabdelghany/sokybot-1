package org.sokybot.game.enums;

public enum JobType {

	None(0) , 
	Trader(1) , 
	Thief(2) , 
	Hunter(3)  , 
	UNKNOWN(-1); 
	
	private byte type  ; 
	
	private JobType(int type) {
	 this.type = (byte) type ; 
	}
	
	
	public static JobType of(byte val) { 
		for(JobType t : values()) { 
			if(t.type == val) return t ; 
		}
		return UNKNOWN ; 
	}
}
