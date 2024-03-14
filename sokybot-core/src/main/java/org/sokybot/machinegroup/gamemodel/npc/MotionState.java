package org.sokybot.machinegroup.gamemodel.npc;

public enum MotionState {

	//0 = None, 2 = Walking, 3 = Running, 4 = Sitting
	
	None(0) , 
	Walking(2) , 
	Running(3) , 
	Sitting(4)  , 
	UNKNOWN(-1) ; 
	
	
	private byte state ; 
	
	private MotionState(int val) {
		this.state =(byte) val ; 
	}
	
	public static MotionState of(byte val) { 
		for(MotionState s : values()) { 
			if(s.state == val) return s ; 
		}
		return UNKNOWN; 
		
	}
}
