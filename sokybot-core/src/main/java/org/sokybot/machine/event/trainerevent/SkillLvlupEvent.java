package org.sokybot.machine.event.trainerevent;

import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.sokybot.machinegroup.gamemodel.skill.SkillEntity;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;

import lombok.Getter;

@Getter
public class SkillLvlupEvent  {

	private Skill skill ; 
	
	public SkillLvlupEvent(Skill skill) {
	 this.skill = skill ; 
	   
	}
	
	
	
}
