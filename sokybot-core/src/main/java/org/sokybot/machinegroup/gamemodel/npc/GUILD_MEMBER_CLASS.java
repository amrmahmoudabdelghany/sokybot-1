package org.sokybot.machinegroup.gamemodel.npc;
public  enum GUILD_MEMBER_CLASS {
	MASTER(0x01) , MEMBER(0x02)  , UNKNOWN(0x00)  ;
	
	private byte code ; 
	
	private GUILD_MEMBER_CLASS(int code) { 
		this.code = (byte)code ; 
	}
	
	public static GUILD_MEMBER_CLASS getGMC(int code) { 
		for(GUILD_MEMBER_CLASS gmc : values()) {
			if(gmc.code == code) return gmc ; 
		}
		
		return UNKNOWN;  
	}
}