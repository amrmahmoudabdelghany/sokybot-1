package org.sokybot.network.packet;

public class ServerOpcode {

	public static final int PATCH_RESPONSE =  0xA100;
	public static final int CHAR_DIE = 0x3011 ; 
	public static final int CHAR_DATA = 0x3013;
	public static final int CHAR_DATA_BEGIN = 0x34A5;  // CharacterData transaction begin
	public static final int CHAR_DATA_END = 0x34A6;    // CharacterData transaction end (triggers full parse)
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
	
	// Entity state updates
	public static final int ENTITY_STATE_UPDATE = 0x30BF;  // Life/Motion/Body state
	
	// Inventory operations
	public static final int INVENTORY_ITEM_UPDATE = 0x3040;  // Item property updates
	public static final int INVENTORY_OPERATION = 0xB034;    // Move/pickup/drop/buy/sell
	
	// Party operations
	public static final int PARTY_UPDATE = 0x3864;           // Party state changes
	
	// Storage operations
	public static final int STORAGE_DATA_BEGIN = 0x3047;     // Storage open/begin (Personal)
	public static final int GUILD_STORAGE_DATA_BEGIN = 0x3253;// Storage open/begin (Guild)
	public static final int STORAGE_DATA = 0x3049;           // Storage item data
	public static final int STORAGE_DATA_END = 0x3048;       // Storage data end
	
	// Quest operations
	public static final int QUEST_UPDATE = 0x30D5;           // Quest state changes
	
	// Action operations
	public static final int ENTITY_DESELECT = 0xB04B;        // Entity deselected
	public static final int NPC_TALK = 0xB046;               // NPC talk dialog opened
	
	// Session operations
	public static final int LOGOUT = 0x300A;                 // Logout success
	public static final int TELEPORT_COMPLETE = 0x34B5;      // Game reset after teleport
	
	// Exchange/Trade operations
	public static final int EXCHANGE_STARTED = 0x3085;       // Player trade started
	public static final int EXCHANGE_CANCELLED = 0x3088;     // Player trade cancelled
	public static final int EXCHANGE_APPROVED = 0x3087;      // Player trade approved
	
	// Pet/Mount operations
	public static final int MOUNT_STATE_UPDATE = 0xB0CB;     // Mount/dismount vehicle
	
	// Job operations
	public static final int JOB_JOIN = 0xB0E1;               // Join job
	public static final int JOB_LEAVE = 0xB0E2;              // Leave job
	
	// Alchemy operations
	public static final int ALCHEMY_ELIXIR = 0xB150;         // Elixir alchemy result
	public static final int ALCHEMY_STONE = 0xB151;          // Stone alchemy result

	// Additional Party operations
	public static final int PARTY_INVITE = 0x3080;           // Party invitation/request
	
	// Additional Inventory operations
	public static final int ITEM_USE = 0xB04C;               // Item used
	public static final int ITEM_DURABILITY = 0x3052;        // Item durability update
	public static final int INVENTORY_SIZE = 0x3092;         // Inventory/Storage size update
	
}
