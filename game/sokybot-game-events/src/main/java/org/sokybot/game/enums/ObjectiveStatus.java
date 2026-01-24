package org.sokybot.game.enums;

public enum ObjectiveStatus {

	DONE(0) , 
	INCOMPLETE(1)  , 
	UNKNOWN(-1); 
	
	private byte status ; 
	
	private ObjectiveStatus(int val) {
	 this.status = (byte) val; 
	}
	
	public static ObjectiveStatus of(byte val) { 
		for(ObjectiveStatus status : values()) { 
			if(status.status == val) return status ; 
		}
		return UNKNOWN ; 
	}
}
