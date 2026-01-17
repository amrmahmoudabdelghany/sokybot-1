package org.sokybot.machine.model.item;

import lombok.Data;

@Data
public class Option {
	
	private byte optSlot;
	private int optID;
	private int optNParam1; // (=> Reference to Socket)
	private int optValue;
}
