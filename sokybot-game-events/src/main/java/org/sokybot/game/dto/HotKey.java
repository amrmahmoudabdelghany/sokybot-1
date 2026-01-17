package org.sokybot.game.dto;

import org.sokybot.game.enums.HKSlotType;
import lombok.Data;

@Data
public class HotKey {

	private byte slotSeq ;
	
	private HKSlotType slotType = HKSlotType.UNKNOWN ; 
	
	private int data ; 
	
}
