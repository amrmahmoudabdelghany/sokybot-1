package org.sokybot.gameevents.dto;

import org.sokybot.gameevents.enums.HKSlotType;
import lombok.Data;

@Data
public class HotKey {

	private byte slotSeq ;
	
	private HKSlotType slotType = HKSlotType.UNKNOWN ; 
	
	private int data ; 
	
}
