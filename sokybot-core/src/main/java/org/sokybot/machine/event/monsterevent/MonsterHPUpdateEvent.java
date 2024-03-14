package org.sokybot.machine.event.monsterevent;

import org.sokybot.machinegroup.gamemodel.npc.Monster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonsterHPUpdateEvent {
	
	private Monster monster ; 
	private int uniqueId ; 
	private int newHP ; 
	
	
	
	public boolean isDead() { 
		return newHP == 0 ; 
	}
	
	

}
