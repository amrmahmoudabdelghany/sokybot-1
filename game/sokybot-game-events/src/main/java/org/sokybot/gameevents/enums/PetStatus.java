package org.sokybot.gameevents.enums;

public enum PetStatus {
	
	Unsummoned(1), Summoned(2), Alive(3), Dead(4) , UNKNOWN(0);

	private byte value;

	private PetStatus(int val) {
		this.value = (byte) val;
	}

	public byte getStatus() {
		return this.value;
	}
	
	public static PetStatus of(byte val) { 
		
		for(PetStatus s : values()) { 
			if(s.value == val) return s ; 
		}
		
		return UNKNOWN ; 
	}
}
