package org.sokybot.machine.parser;

import org.sokybot.machinegroup.gamemodel.item.Item;
import org.sokybot.machinegroup.gamemodel.npc.HotKey;
import org.sokybot.machinegroup.gamemodel.quest.Quest;
import org.sokybot.machinegroup.gamemodel.skill.Buff;
import org.sokybot.machinegroup.gamemodel.skill.Mastery;
import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.sokybot.network.packet.IStreamReader;

public interface ICharacterDataReader extends IStreamReader {

	Skill getSkill();

	Buff getBuff();

	Quest getQuest();

	Item getItem();

	Mastery getMastery() ; 
	
	HotKey getHotKey() ; 
	
}
