package org.sokybot.network.packet;

public class ClientOpcode {

	public static final int AGENT_REQUEST = 0x6101;
	/** Gateway session: client build / version (vSRO-style), after server module id. */
	public static final int GATEWAY_CLIENT_BUILD = 0x6100;
	public static final int LOGIN_REQUEST = 0x6102;
	/** Gateway image / CAPTCHA answer (vSRO-style, after server 0x2322). */
	public static final int GATEWAY_IMAGE_CODE_ANSWER = 0x6323;
	public static final int AUTH_REQUEST = 0x6103;
	public static final int LOGOUT_REQUEST = 0x6104;
	/** Patch / keepalive ping during gateway session (many servers expect this during login). */
	public static final int GATEWAY_PATCH_PING = 0x2002;
	public static final int JOIN_REQUEST = 0x7001 ; 
	public static final int CHAT_REQUEST = 0x7025;
	public static final int CHAR_MOVEMENT = 0x7021 ; 
	public static final int CHAR_SELECT = 0x7045 ; 
	public static final int CHAR_ACTION = 0x7074 ; 
	public static final int CHAR_SKILL_LVL_UP = 0x70A1 ; 
	public static final int CHAR_MASTERY_LVL_UP = 0x70A2 ; 
	public static final int CHAR_BESERK = 0x70A7 ; 

	/** Player-to-player exchange / trade (vSRO-style client opcodes). */
	public static final int EXCHANGE_REQUEST = 0x7081;
	public static final int EXCHANGE_CONFIRM = 0x7082;
	public static final int EXCHANGE_ADD_ITEM = 0x7083;
	public static final int EXCHANGE_APPROVE = 0x7084;
	public static final int EXCHANGE_CANCEL = 0x7085;
	public static final int EXCHANGE_FINALIZE = 0x7086;
	
}
