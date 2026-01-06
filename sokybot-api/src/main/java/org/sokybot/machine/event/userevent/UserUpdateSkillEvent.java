package org.sokybot.machine.event.userevent;

import org.sokybot.persistence.entities.SkillEntity;

import lombok.Getter;

@Getter
public class UserUpdateSkillEvent {
 
	private Object source ; 
	 
	private SkillEntity skillEntity ;
	
	
	public UserUpdateSkillEvent(Object source , SkillEntity entity) { 
		this.source = source ; 
		this.skillEntity = entity ; 
	}
	
    public SkillEntity getSkillEntity() {
        return this.skillEntity;
    }
    
    public Object getSource() {
        return this.source;
    }
	
}
