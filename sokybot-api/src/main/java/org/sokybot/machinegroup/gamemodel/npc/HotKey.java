package org.sokybot.machinegroup.gamemodel.npc;

import lombok.Data;

@Data
public class HotKey {

	private byte slotSeq ;
	
	private HKSlotType slotType = HKSlotType.UNKNOWN ; 
	
	private int data ; 
	
}
