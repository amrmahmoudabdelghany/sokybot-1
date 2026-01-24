package org.sokybot.game.enums;

public enum DebuffStatus {

	Normal(0) , 
	BadStatus(1) , 
	Debuff(2) ,
	UNKNOWN(-1) ; 
	
	private byte status ; 
	
	private DebuffStatus(int val) {
		this.status = (byte) val ;
	}
	
	public static DebuffStatus of(byte val) { 
		
		for(DebuffStatus ds : values() ) {
			if(ds.status == val) return ds ; 
		}
		
		return UNKNOWN ; 
	}
}
