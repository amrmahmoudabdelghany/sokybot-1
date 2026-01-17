package org.sokybot.game.enums;

public enum GuildMemberClass {
	MASTER(0x01) , MEMBER(0x02)  , UNKNOWN(0x00)  ;
	
	private byte code ; 
	
	private GuildMemberClass(int code) { 
		this.code = (byte)code ; 
	}
	
	public static GuildMemberClass getGMC(int code) { 
		for(GuildMemberClass gmc : values()) {
			if(gmc.code == code) return gmc ; 
		}
		
		return UNKNOWN;  
	}
}
