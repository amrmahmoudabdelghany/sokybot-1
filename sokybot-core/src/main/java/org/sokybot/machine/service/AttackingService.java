package org.sokybot.machine.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.sokybot.machine.event.DespawnEvent;
import org.sokybot.machine.event.SkillCastErrorEevent;
import org.sokybot.machine.event.SpawnReachDestinationEvent;
import org.sokybot.machine.event.monsterevent.MonsterHPUpdateEvent;
import org.sokybot.machine.event.monsterevent.MonsterSelectedEvent;
import org.sokybot.machine.event.monsterevent.MonsterSpawnEvent;
import org.sokybot.machine.event.userevent.UserConfigUpdatedEvent;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machinegroup.gamemodel.npc.MonsterType;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class AttackingService implements IAttackingService {

	@Autowired
	private ITrainerManager trainerManager;

	@Autowired
	private Settings settings;

	@Autowired
	private Trainer trainer;

	@Autowired
	private Logger log;

	private Monster selectedMonster;
	
	
	private List<String> attackList;
	private int skillPtr = 0;

	
	private Skill nextSkill() {

		if (this.attackList.isEmpty())
			return null;

		Skill res = null;

		for (skillPtr = skillPtr % this.attackList.size(); skillPtr < this.attackList.size(); skillPtr++) {
			String skillName = this.attackList.get(skillPtr);

			Skill s = this.trainer.findSkill(skillName).orElse(null);
			if (s != null && s.isEnabled()) {
				skillPtr++;
				res = s;
				break;
			}
		}

		return res;
	}

	
	



	@Override
	public boolean isEmergencySituation() {
		return false;
	}

	@Override
	public void attack() {

		if (this.selectedMonster == null)
			throw new IllegalStateException("Attacking while no selected monster");

		Skill skill = nextSkill();

		if (skill == null) {
			if (this.settings.isAutoAttack())
				this.trainerManager.attack(this.selectedMonster.getUniqueId());
		} else {
			this.trainerManager.useSkill(skill.getRefId(), this.selectedMonster.getUniqueId());
		}
	}

	@EventListener
	public void onMonsterSelected(MonsterSelectedEvent event) {
		this.selectedMonster = event.getSelectedMonster();
		initAttackSkills();

	}

	

	

	@EventListener
	public void onCastError(SkillCastErrorEevent event) {
		String error = "";

		if (event.getErrorType() == SkillCastErrorEevent.OBSTACLE) {
			error = "Counter Obstacle";
			// here we can get the path between target monster and trainer go to it 
		} else if (event.getErrorType() == SkillCastErrorEevent.INVALID_TARGET) {
			error = "Invalid Target";
		} else if (event.getErrorType() == SkillCastErrorEevent.SKILL_ON_COOLDOWN) {
			error = "Skill On Cooldown";
		}

		log.info("Error while cast skill {} ", error);
	}

	private void initAttackSkills() {

		// log.info("AttackingActuator.initAttackSkills , ptr : {} " , this.skillPtr);
		List<String> attackSkills = this.settings.getAttakListFor(this.selectedMonster.getMonsterType());

		/// this.attackList =
		/// this.config.getAttakListFor(this.targetMonster.getMonsterType());

		if (attackSkills.isEmpty()) {
			attackSkills = this.settings.getAttakListFor(MonsterType.Normal);
		}

		if (attackSkills == this.attackList) { // working with the same attack skill list

			if (this.settings.isIterateSkillsPerMonster()) {
				this.skillPtr = 0;

			}
		} else {

			this.attackList = attackSkills;
			this.skillPtr = 0;
		}

	}

	

}
