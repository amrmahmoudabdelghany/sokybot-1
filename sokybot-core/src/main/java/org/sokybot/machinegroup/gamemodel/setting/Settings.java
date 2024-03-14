package org.sokybot.machinegroup.gamemodel.setting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKey;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machinegroup.gamemodel.npc.MonsterType;
import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.springframework.context.annotation.Bean;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static org.sokybot.machinegroup.gamemodel.setting.MonsterPreference.* ; 

@Data
@Entity
@NoArgsConstructor
public class Settings implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	// @Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	@Column(name ="id")
	private String id;

	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private String groupName ; 
	
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private String trainerName ; 
	
	
	private String targetGateway = "";

	private String username = "";
	private String password = "";
	private String passcode = "";
	private String targetAgent = "";

	private BotType botType = BotType.CLIENT;

	private boolean autoLogin = false;
	private boolean doNotAttack = false;

	private boolean autoAttack = true;
	private boolean iterateSkillsPerMonster = true;

	private boolean preferAttackerMonster = true ;
	
	@OneToMany(mappedBy = "settings", fetch = FetchType.EAGER, cascade = { CascadeType.ALL }, orphanRemoval = true)
	// @MapKey(name = "monsterType")
	// @MapKeyEnumerated(EnumType.STRING)
	// @ElementCollection(fetch = FetchType.EAGER )
	// @Cascade(CascadeType.ALL)
	@Setter(value = AccessLevel.NONE)
	private Map<MonsterType, SkillList> attackList = new HashMap<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@Setter(value = AccessLevel.NONE)
	private Map<MonsterType, MonsterPreference> monsterPreferences = new HashMap<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@Setter(value = AccessLevel.NONE)
	private Map<String, MonsterPreference> monsterNamePreferences = new HashMap<>();

	@OneToOne(mappedBy = "settings" ,cascade = {CascadeType.ALL})
	@Getter(value = AccessLevel.NONE)
	private TrainingAreaSettings areaSettings  ; 
	
	public Settings( String id, String groupName , String  trainerName) {
		this.id = id ; 
		this.groupName = groupName ; 
		this.trainerName = trainerName ; 	 
		this.areaSettings = new TrainingAreaSettings(this , trainerName) ;
		System.out.println("On Constract Settting Object" ) ; 
	}

	
	public TrainingAreaSettings getTrainingAreaSettings() { 
		return this.areaSettings ; 
	}
	
	public void addSkill(MonsterType type, String skill) {
		MonsterType target = type;

		if (target == MonsterType.Elite1 || target == MonsterType.Elite2) {
			target = MonsterType.Elite;
		}

		SkillList skillList = this.attackList.get(target);
		if (skillList == null) {
			skillList = new SkillList(this);
			attackList.put(target, skillList);
		}
		skillList.add(skill);

	}

	public List<String> getAttakListFor(MonsterType type) {

		MonsterType target = type;

		if (target == MonsterType.Elite1 || target == MonsterType.Elite2) {
			target = MonsterType.Elite;
		}

		SkillList skillList = this.attackList.get(target);

		if (skillList == null) {
			return new ArrayList<>();
		}
		return skillList.getSkillList();

	}

	
	

	
	
	
	
		
	public void removeSkill(MonsterType selectedMonsterType, String target) {

		List<String> skillList = getAttakListFor(selectedMonsterType);
		skillList.remove(target);

	}

	public void setMonsterPreference(MonsterType type, MonsterPreference preference) {

		this.monsterPreferences.put(type, preference);

	}

	public void setMonsterPreference(String mobName, MonsterPreference preference) {

		this.monsterNamePreferences.put(mobName, preference);

	}

	public MonsterPreference getMonsterPreference(String mobName) {

		return this.monsterNamePreferences.getOrDefault(mobName, NONE);

	}

	public MonsterPreference getMonsterPreference(MonsterType type) {

		if (this.monsterPreferences == null)
			return NONE;

		return this.monsterPreferences.getOrDefault(type, NONE);

	}
	public MonsterPreference getMonsterPreference(Monster mob) { 
		MonsterPreference byType = getMonsterPreference(mob.getMonsterType()) ; 
		MonsterPreference byName = getMonsterPreference(mob.getName()) ; 
		//System.out.println("Monster : " + mob.getName() + " is : " + byName.name()) ; 
		if(byType == AVOID || byName == AVOID) return AVOID ; 
		if(byType == PREFER || byName == PREFER) return PREFER ;
		return NONE ; 
	}
	
	public boolean isAvoided(Monster mob) { 
	   return getMonsterPreference(mob) == AVOID ; 	
	}

	public void swapAttackSkill(MonsterType type, int index1, int index2) {

		List<String> target = getAttakListFor(type);

		int len = target.size();

		if (index1 < 0 || index2 < 0)
			return;

		if (index1 >= len || index2 >= len)
			return;

		String skill1 = target.get(index1);
		String skill2 = target.get(index2);
		target.set(index1, skill2);
		target.set(index2, skill1);

	}

//	@Getter(value = AccessLevel.NONE)
//	@Setter(value = AccessLevel.NONE)
//	@ElementCollection(fetch = FetchType.EAGER)
//	private List<String> buffSkill = new ArrayList<>();

	// public Skill imbueSkill = null;

	public static void main(String args[]) {

		Map<String, String> hashMap = new HashMap<>();
		hashMap.put(null, "test");

	}

}
