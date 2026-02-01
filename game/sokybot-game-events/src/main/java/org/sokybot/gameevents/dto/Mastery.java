package org.sokybot.gameevents.dto;

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
    
    public int getMasteryID() { return masteryID; }
    public void setMasteryID(int masteryID) { this.masteryID = masteryID; }
    
    public byte getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(byte masteryLevel) { this.masteryLevel = masteryLevel; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getId() { return masteryID; }
}
