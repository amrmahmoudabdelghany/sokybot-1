package org.sokybot.gameevents.enums;

public enum Rarity {

	Normal(0) , 
	Blue(1) , 
	Sox(2)  , 
	UNKNOWN(-1); 
	
	private byte val ; 
	
	private Rarity(int val) {
		this.val =(byte) val ; 
	}
	
	public static Rarity of(byte val) { 
	
		for(Rarity r : values()) { 
			if(r.val == val ) return r ; 
		}
		
		return UNKNOWN ; 
	}
}
