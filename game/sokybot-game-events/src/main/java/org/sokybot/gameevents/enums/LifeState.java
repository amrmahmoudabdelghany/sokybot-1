package org.sokybot.gameevents.enums;

public enum LifeState {

	//1 = Alive, 2 = Dead
	
	Alive(1) , Dead(2) , 
	UNKNOWN(0) ; 
	
	private byte state  ; 
	
	private LifeState(int stateVal) {
	 this.state =(byte) stateVal ; 
	}
	
	public static LifeState of(byte val) { 
		for(LifeState s : values()) { 
			if(s.state == val) return s; 
		}
		return UNKNOWN ; 
		
	}
	
}
