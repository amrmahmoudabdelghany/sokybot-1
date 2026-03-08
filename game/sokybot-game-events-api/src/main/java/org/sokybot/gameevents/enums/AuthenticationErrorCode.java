package org.sokybot.gameevents.enums;


public enum AuthenticationErrorCode  {

	UNKNOWN(0) , 
	ServerIsFull(4) , 
	IPLimit(5) ; 
	
	private byte code ; 
	
	private AuthenticationErrorCode(int code) { 
		this.code= (byte) code ; 
	}
	
	public static AuthenticationErrorCode getError(byte code) { 
		for(AuthenticationErrorCode error : values()) { 
			if(error.code == code) return error ; 
		}
		return UNKNOWN ; 
	}
	
	
}
