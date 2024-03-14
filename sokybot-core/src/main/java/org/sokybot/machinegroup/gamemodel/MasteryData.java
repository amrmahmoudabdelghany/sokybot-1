package org.sokybot.machinegroup.gamemodel;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MasteryData {

	@Id
	private int masteryId ; 
	
	private String name ; 
	
	
	
}
