package org.sokybot.game.enums;

public enum InteractionMode {

	None(0) , 
	P2P(2) , 
	P2N_TALK(4) , 
	OPNMKT_DEAL(6) ; 
	
	private byte val ; 
	
	private InteractionMode(int val) {
		this.val =(byte) val ; 
	}
	
	public static InteractionMode of(byte val) { 
		for(InteractionMode m : values()) { 
			if(m.val == val) return m ;
		}
		return None ; 
	}
}
