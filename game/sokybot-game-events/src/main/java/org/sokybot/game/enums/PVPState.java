package org.sokybot.game.enums;

public enum PVPState {

	White(0), Purple(1), Red(2), UNKNOWN(-1);

	private byte state ; 
	
	private PVPState(int val) {
		this.state = (byte) val ; 
	}
	
	public static PVPState  of(byte val) { 
		
		for(PVPState s : values()) { 
			if(s.state == val) return s ; 
		}
		
		return UNKNOWN ; 
	}
}
