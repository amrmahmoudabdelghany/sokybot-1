package org.sokybot.network.packet;

public class ClientOpcode {

	public static final int AGENT_REQUEST = 0x6101;
	public static final int LOGIN_REQUEST = 0x6102;
	public static final int AUTH_REQUEST = 0x6103;
	public static final int LOGOUT_REQUEST = 0x6104;
	public static final int JOIN_REQUEST = 0x7001 ; 
	public static final int CHAT_REQUEST = 0x7025;
	public static final int CHAR_MOVEMENT = 0x7021 ; 
	public static final int CHAR_SELECT = 0x7045 ; 
	public static final int CHAR_ACTION = 0x7074 ; 
	public static final int CHAR_SKILL_LVL_UP = 0x70A1 ; 
	public static final int CHAR_MASTERY_LVL_UP = 0x70A2 ; 
	public static final int CHAR_BESERK = 0x70A7 ; 
	
}
