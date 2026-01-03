package org.sokybot.machinegroup.gamemodel.login;

import org.sokybot.machine.IMachineEvent;

public enum AuthenticationErrorCode implements IMachineEvent {

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
