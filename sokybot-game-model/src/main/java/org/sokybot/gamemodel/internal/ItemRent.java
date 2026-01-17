package org.sokybot.gamemodel.internal;

import lombok.Data;

@Data
public class ItemRent {

	private int periodBeginTime ; 
	
	private int periodEndTime ; 
	
	private short canDelete ; 
	
	private int meterRateTime ; 
	
	private short canRecharge ; 
	
	private int packingTime ;
	
}
