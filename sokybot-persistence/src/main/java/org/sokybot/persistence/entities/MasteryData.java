package org.sokybot.persistence.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MasteryData {

	@Id
	private int masteryId ; 
	
	private String name ; 
	
	public int getMasteryId() {
		return masteryId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setMasteryId(int masteryId) {
		this.masteryId = masteryId;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	
}
