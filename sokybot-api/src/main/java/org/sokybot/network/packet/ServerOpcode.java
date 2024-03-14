package org.sokybot.network.packet;

public class ServerOpcode {

	public static final int PATCH_RESPONSE =  0xA100;
	public static final int CHAR_DIE = 0x3011 ; 
	public static final int CHAR_DATA = 0x3013;
	public static final int CHAR_INFO = 0x303D; 
	public static final int CHAR_MASTERY_LVL_UP = 0xB0A2 ; 
	public static final int CHAR_SKILL_LVL_UP =  0xB0A1 ; 
	public static final int SKILL_CAST_STARTED = 0xB070 ; 
	public static final int SKILL_CAST_ENDED = 0xB071 ; 
	public static final int SKILL_CAST_CONFIRM = 0xB074 ;  
	public static final int AGENT_LIST =  0xA101;
	public static final int LOGIN_RESPONSE =  0xA102 ; 
	public static final int AUTH_RESPONSE =  0xA103 ; 
	public static final int CHAT_UPDATE = 0x3026 ; 
	public static final int ATTACK_GAINS_UPDATE = 0x304E ; 
	public static final int EXP_SP_UPDATE = 0x3056 ;
	public static final int SINGLE_SPAWN = 0x3015 ;
	public static final int GROUP_SPAWN_BEGIN = 0x3017 ; 
	public static final int GROUP_SPAWN = 0x3019 ; 
	public static final int GROUP_SPAWN_END = 0x3018;
	public static final int SPAWN_MOVEMENT = 0xB021; 
	public static final int SPAWN_STUCK = 0xB023 ; 
	
	public static final int SPAWN_SELECTED = 0xb045 ; 
	public static final int SPEED_UPDATE = 0x30D0 ;
	public static final int SINGLE_DESPAWN = 0x3016;
	public static final int ANGLE_UPDATE = 0xB024;
	public static final int HPMP_UPDATE = 0x3057; 
	public static final int BESERK_CONFIRM = 0xB0A7 ; 
	
	
}
