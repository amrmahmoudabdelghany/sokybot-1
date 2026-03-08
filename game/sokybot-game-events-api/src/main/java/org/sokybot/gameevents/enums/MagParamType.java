package org.sokybot.gameevents.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum MagParamType {

	UNKNOWN( 0x00)  ,  
	Immortal(0x30) , 
	Steady(0x38) , 
	Lucky(0x3B), 
    Str(0x48), 
    Int(0x49), 
    Durability(0x5A), 
    HP(0x45),  
    MP(0x4B),  
    AttackRatio(0x66), 
    BlockRatio(0x72),  
    CriticalBlock(0x7E),  
    ParryRatio(0x8A),  
    Burn(0xA8), 
    ElectricShock(0xAE), 
    Freeze(0xB4),  
    Poison(0xBA), 
    Zombie(0xC0);
    
	
	private int value ; 
	
	private MagParamType(int val) {
		this.value = val ; 
	}
	
	private static Map<Integer, MagParamType> values = Arrays.asList(values()).stream().collect(Collectors.toMap((v)->v.value, (v)->v)) ;  
	
	public static MagParamType of(int val) { 
		
		return values.getOrDefault(val , UNKNOWN) ; 
	}
}
