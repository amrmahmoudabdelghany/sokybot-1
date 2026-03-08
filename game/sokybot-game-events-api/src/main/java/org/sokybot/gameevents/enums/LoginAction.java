package org.sokybot.gameevents.enums;

public enum LoginAction {

	CREATE(0x01), LIST(0x02), DELETE(0x03), CHECK_NAME(0x04), RESTORE(0x05), UNKNOWN(0);

	private int code;

	private LoginAction(int code) {
		this.code = code;
	}
	
	public static LoginAction getAction(int code) { 
		for(LoginAction action : values()) { 
			if(action.code == code) return action ; 
		}
		return UNKNOWN ; 
		
	}

}
