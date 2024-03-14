package org.sokybot.machine.event.monsterevent;

import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MonsterDespawnEvent extends ApplicationEvent {

	private Monster monster;

	public MonsterDespawnEvent(Object source, Monster monster) {
		super(source);
		this.monster = monster;

	}
}
