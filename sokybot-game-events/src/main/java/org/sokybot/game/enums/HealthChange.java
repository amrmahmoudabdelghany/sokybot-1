package org.sokybot.game.enums;

import java.util.Map;

import org.sokybot.machine.IMachineEvent;

public enum HealthChange implements IMachineEvent {

	HPChanged, MPChanged, 
	HPAndMPChanged, BadStatus, 
	HPAndBadStatusOrMonster,
	MPAndBadStatus , 
	UNKNOWN;

	private static Map<Byte, HealthChange> changes = Map
			.of((byte)0x01, HealthChange.HPChanged,
					(byte)0x02, HealthChange.MPChanged,
					(byte)0x03, HealthChange.HPAndMPChanged,
					(byte)0x04, HealthChange.BadStatus,
					(byte)0x05, HealthChange.HPAndBadStatusOrMonster,
					(byte)0x06, HealthChange.MPAndBadStatus) ; 
	
	public static  HealthChange of(byte val) { 
		return changes.getOrDefault(val, UNKNOWN) ; 
	}
	
	
}
