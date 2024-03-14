package org.sokybot.machinegroup.gamemodel.skill;

import lombok.Data;

@Data
public class Mastery {
	private String name ; 
	private int masteryID;
	private byte masteryLevel;
	
	
	
	@Override
	public String toString() { 
		return name + "( " + masteryLevel + " ) " ; 
	}
}