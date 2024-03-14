package org.sokybot.machine.model;

import org.sokybot.machine.IMachineEvent;

public enum ClientFeed implements IMachineEvent {

	CLIENT_CONNECTED ,
	CONNECTION_REFUSED , 
	CONNECTION_ACCEPTED  , 
	GAME_READY , 
	UNKNOWN; 
	
	public static ClientFeed of(int opcode) { 
		return UNKNOWN ; 
	}
}
