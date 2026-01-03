package org.sokybot.machinegroup.gamemodel.npc;

public enum CharacterStatus {

	/*
	
 (0 = None, 1 = Zerk, 2 = Untouchable, 3 = GMInvincible, 4 = GMInvisible, 5= ??, 6 = Stealth, 7 = Invisible)	
	*/
	
	
	
	None(0) , 
	Zerk(1) , 
	Untouchable(2) ,  
	GameMasterInvincible(3) , 
	GameMasterInvisible(4) ,
	Stealth(6) , 
	Invisible(7) , 
	UNKNWON(-1);
	
	private byte status ; 
	
	private CharacterStatus(int val ) { 
		this.status =(byte) val ; 
	}
	
	public static CharacterStatus of(byte val) { 
		
		for(CharacterStatus s : values()) {
			if(s.status == val) return s ; 
		}
		
		return UNKNWON ; 
	}
	
	
	
}
