package org.sokybot.gameevents.enums;

public enum FreePVP {

	//0 = None, 1 = Red, 2 = Gray, 3 = Blue, 4 = White, 5 = Gold
	
	None(0) , 
	Red(1) ,
	Gray(2) , 
	Blue(3) , 
	White(4) , 
	Gold(5) , 
	UNKNOWN(-1); 
	
	private byte pvp ; 
	
	private FreePVP(int val) {
		this.pvp =(byte) val ; 
	}
	
	public static FreePVP of(byte val) { 
		
		for(FreePVP fp : values()) { 
			if(fp.pvp == val) return fp ; 
		}
		
		return UNKNOWN ; 
	}
}
