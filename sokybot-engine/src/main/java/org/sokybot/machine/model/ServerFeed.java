package org.sokybot.machine.model;

import java.util.HashMap;
import java.util.Map;

import org.sokybot.machine.IMachineEvent;
import org.sokybot.network.packet.ServerOpcode;

public enum ServerFeed implements IMachineEvent {

	SETUP ,
	CHALLENGE , 
	INCOMPATIBLE , 
	COMPATIBLE , 
    GATEWAY_CONNECTED ,
	AGENT_CONNECTED , 
	AGENT_lISTED , 
	LOGIN_SUCCESS , 
	AUTHENTICATED , 
	LISTED , 
	JOINED  , 
	UNKNOWN; 
	
	private static Map<Integer, ServerFeed>  map = new HashMap<>() ;
	
	static  {
	 
		map.put(ServerOpcode.AGENT_LIST , AGENT_lISTED) ; 
		
		
	}
	public static ServerFeed of(int opcode) { 
		
		return map.getOrDefault(opcode, UNKNOWN) ; 
	}
}
