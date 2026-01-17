package org.sokybot.machine.service;

import org.sokybot.game.dto.Skill;

public interface ITrainerManager {

	
	
	void walk(int  tragetX , int  targetY) ; 
	
	void walkInCave(int targetX , int targetY) ; 
	
	void attack(int monsterId) ; 
	
	
	void useSkill(int skillId , int targetId ) ; 
	
	void useSkill(int skillId) ; 
	
	void select(int npcId) ; 
	
	void levelUpMastery(int masteryID) ; 
	void levelUpSkill(int skillId) ; 
	
	void enterBerserkMode() ; 
	
}
