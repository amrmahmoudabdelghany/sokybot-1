package org.sokybot.machine.event.monsterevent;

import org.sokybot.machinegroup.gamemodel.npc.Monster;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class MonsterSelectedEvent {
	
	private Monster selectedMonster; 
	
	

}
