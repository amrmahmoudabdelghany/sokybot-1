package org.sokybot.machinegroup.gamemodel.login;

public enum Action {

	CREATE(0x01), LIST(0x02), DELETE(0x03), CHECK_NAME(0x04), RESTORE(0x05), UNKNOWN(0);

	private int code;

	private Action(int code) {
		this.code = code;
	}
	
	public static Action getAction(int code) { 
		for(Action action : values()) { 
			if(action.code == code) return action ; 
		}
		return UNKNOWN ; 
		
	}

}
