package org.sokybot.machinegroup.gamemodel;

public enum Race {
 

	Chinese((byte)0x00),
	Euro((byte)0x01),
    Universal((byte)0x02) ; 
	
	private byte value ; 
	
	
	
	public byte getValue() { 
		return value ; 
	}
    Race(byte val) {
	
    	value = val ; 
	}
    
    public static Race parseType(byte val) { 
    	for(Race r : values()) { 
    		if(r.getValue() == val) return r ; 
    	}
    	return Race.Universal ; 
    }
    
}
