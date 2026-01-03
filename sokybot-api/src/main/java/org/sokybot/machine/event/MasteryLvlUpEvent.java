package org.sokybot.machine.event;

import org.sokybot.machinegroup.gamemodel.skill.Mastery;
import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class MasteryLvlUpEvent extends ApplicationEvent {

	private Mastery mastery ; 
	
	public MasteryLvlUpEvent(Object source , Mastery mastery) {
	  super(source) ; 

	  this.mastery = mastery ; 
	}
	

	
	
	
}
