package org.sokybot.machine.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.sokybot.gameevents.events.spawn.MonsterSpawnEvent;
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent;
import org.sokybot.gameevents.events.entity.EntitySelectedEvent;
import org.sokybot.machine.event.userevent.UserConfigUpdatedEvent;
import org.sokybot.game.dto.Skill;
import org.sokybot.game.enums.SkillCastErrorType;
import org.sokybot.gameevents.events.skill.SkillCastErrorEvent;
import org.sokybot.machine.model.Trainer;
import org.sokybot.machine.model.Monster;
import org.sokybot.settings.MonsterType;
import org.sokybot.settings.Settings;
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
    private org.sokybot.machine.model.IGameModel gameModel;

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
	public void onMonsterSelected(EntitySelectedEvent event) {
        this.gameModel.find(event.getSelectedEntityId()).ifPresent(spawn -> {
            if (spawn instanceof Monster) {
                this.selectedMonster = (Monster) spawn;
                initAttackSkills();
            }
        });
	}

	

	

	public void onCastError(SkillCastErrorEvent event) {
		String error = "";

		if (event.getErrorType() == SkillCastErrorType.OBSTACLE) {
			error = "Counter Obstacle";
			// here we can get the path between target monster and trainer go to it 
		} else if (event.getErrorType() == SkillCastErrorType.INVALID_TARGET) {
			error = "Invalid Target";
		} else if (event.getErrorType() == SkillCastErrorType.SKILL_ON_COOLDOWN) {
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
