package org.sokybot.gameevents.enums;


/**
 * ref  https://github.com/DummkopfOfHachtenduden/SilkroadDoc/wiki/ChatType
 * @author Amr
 *
 */
public enum ChatType 
{
	
	
    /**
     General chat visible to nearby entities
    */
    All(1),
    
    /**
    ///$
    */
    PM(2),
    
    /**
    /// General (GM) chat visible to nearby entities
    */
    AllGM(3),
    
    /**
    ///#
    */
    Party(4),
    
    /**
    /// @
    */
    Guild(5), 
    
    /**
    /// Global chatting item
    */
    Global(6),
    
    /**
    /// ~
    */
    Notice(7),
    
    /**
    /// While in stall window
    */
    Stall(9), 
    
    /**
    /// %
    */
    Union(11),
    
    /**
    /// SN_TALK_QNO_EU_EASTEU_21_14
    */
    NPC(13),
    
    /**
    /// &
    */
    Accademy(16)  , 
    
    UNKNOWN(0); 
    
    private byte type ; 
    
    
    private ChatType(int val) { 
     this.type =(byte) val ; 
    }
    
    public byte getTypeValue() { 
    	return this.type ; 
    }
    
    public static ChatType of(byte val) { 
    	for(ChatType t : values()) { 
    		if(t.type == val) return t ; 
    	}
    	
    	return UNKNOWN ; 
    	
    }
    
}
