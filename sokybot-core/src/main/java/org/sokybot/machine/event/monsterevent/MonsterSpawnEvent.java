package org.sokybot.machine.event.monsterevent;

import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MonsterSpawnEvent extends ApplicationEvent {

	private Monster monster ; 
	
	public MonsterSpawnEvent(Object source , Monster monster) {
	  super(source) ; 
	  this.monster = monster ; 
	  
	}
}
