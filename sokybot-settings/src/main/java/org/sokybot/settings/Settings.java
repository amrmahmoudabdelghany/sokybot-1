package org.sokybot.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sokybot.persistence.entities.MonsterType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import static org.sokybot.settings.MonsterPreference.*;

@Data
@NoArgsConstructor
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Settings implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter(value = AccessLevel.NONE)
    private String id; // Machine ID

    @Setter(value = AccessLevel.NONE)
    private String groupName;

    @Setter(value = AccessLevel.NONE)
    private String trainerName;

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

    private boolean preferAttackerMonster = true;

    @Setter(value = AccessLevel.NONE)
    private Map<MonsterType, SkillList> attackList = new HashMap<>();

    @Setter(value = AccessLevel.NONE)
    private Map<MonsterType, MonsterPreference> monsterPreferences = new HashMap<>();

    @Setter(value = AccessLevel.NONE)
    private Map<String, MonsterPreference> monsterNamePreferences = new HashMap<>();

    @Getter(value = AccessLevel.NONE)
    private TrainingAreaSettings areaSettings;

    public Settings(String id, String groupName, String trainerName) {
        this.id = id;
        this.groupName = groupName;
        this.trainerName = trainerName;
        this.areaSettings = new TrainingAreaSettings(this, trainerName);
    }

    public TrainingAreaSettings getTrainingAreaSettings() {
        return this.areaSettings;
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

    public void replaceAttackSkills(MonsterType type, List<String> skills) {
        MonsterType target = type;
        if (target == MonsterType.Elite1 || target == MonsterType.Elite2) {
            target = MonsterType.Elite;
        }

        SkillList skillList = this.attackList.get(target);
        if (skillList == null) {
            skillList = new SkillList(this);
            attackList.put(target, skillList);
        }
        skillList.getSkillList().clear();
        skillList.getSkillList().addAll(skills);
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

    /**
     * Helper to resolve preference by checking both Type and Name.
     * Logic: AVOID takes precedence over PREFER.
     */
    public MonsterPreference getMonsterPreference(MonsterType type, String name) {
        MonsterPreference byType = getMonsterPreference(type);
        MonsterPreference byName = getMonsterPreference(name);
        if (byType == AVOID || byName == AVOID) return AVOID;
        if (byType == PREFER || byName == PREFER) return PREFER;
        return NONE;
    }

    /*
    public boolean isAvoided(Monster mob) {
        return getMonsterPreference(mob) == AVOID;
    }
    */

    public void swapAttackSkill(MonsterType type, int index1, int index2) {
        List<String> target = getAttakListFor(type);
        int len = target.size();
        if (index1 < 0 || index2 < 0) return;
        if (index1 >= len || index2 >= len) return;

        String skill1 = target.get(index1);
        String skill2 = target.get(index2);
        target.set(index1, skill2);
        target.set(index2, skill1);
    }
}
