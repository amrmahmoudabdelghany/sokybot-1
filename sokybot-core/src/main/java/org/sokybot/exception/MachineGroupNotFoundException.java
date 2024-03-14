package org.sokybot.exception;

import lombok.Getter;

@Getter
public class MachineGroupNotFoundException  extends SokybotException{

	private String targetGroupName ; 
	
	public MachineGroupNotFoundException(String message , String targetGroupName ) {
	  super(message) ; 
	  this.targetGroupName = targetGroupName ; 
	}
	
	
	
	
}
