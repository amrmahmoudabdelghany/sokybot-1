package org.sokybot.machine.event.userevent;

import org.sokybot.machinegroup.gamemodel.skill.SkillEntity;

import lombok.Getter;

@Getter
public class UserUpdateSkillEvent {
 
	private Object source ; 
	 
	private SkillEntity skillEntity ;
	
	
	public UserUpdateSkillEvent(Object source , SkillEntity entity) { 
		this.source = source ; 
		this.skillEntity = entity ; 
	}
	
	
}
