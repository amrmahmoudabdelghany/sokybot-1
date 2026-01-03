package org.sokybot.machinegroup.gamemodel.npc;

// for more readable code
public enum MovementType {

	Walking(0), Running(1) , UNKNOWN(-1);

	private byte type;

	private MovementType(int val) {
		this.type =(byte) val;
	}

	public static MovementType of(byte val) { 
		for(MovementType t : values()) { 
			if(t.type == val) return t ; 
		}
		return UNKNOWN ; 
	}
	
}
