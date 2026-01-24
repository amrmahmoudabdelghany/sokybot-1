package org.sokybot.actuator.training;

import lombok.Data;
import org.sokybot.settings.MonsterPreference;
import org.sokybot.settings.MonsterType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings for training actuator.
 */
@Data
public class TrainingSettings {
    
    private boolean autoAttack = true;
    private boolean doNotAttack = false;
    private boolean iterateSkillsPerMonster = true;
    private boolean preferAttackerMonster = true;
    
    private Map<MonsterType, List<String>> attackSkills = new HashMap<>();
    private Map<MonsterType, MonsterPreference> monsterPreferences = new HashMap<>();
    
    // Healing Settings
    private int hpPotionThreshold = 50;
    private int mpPotionThreshold = 50;
    private int hpPetPotionThreshold = 50;
    private boolean useHpPotion = true;
    private boolean useMpPotion = true;
    private boolean usePetPotion = true;
    
    // Navigation / Town Loop Settings
    private boolean loopInTown = true;
    private String scriptPath = "";
    private boolean reverseReturnScroll = false;
    
    // Training area settings
    private String activeAreaName = "";
    private int areaX = 0;
    private int areaY = 0;
    private int areaRadius = 0;
    
    /**
     * Add a skill to the attack list for a monster type.
     */
    public void addSkill(MonsterType type, String skill) {
        attackSkills.computeIfAbsent(type, k -> new ArrayList<>()).add(skill);
    }
    
    /**
     * Get attack skills for a monster type.
     */
    public List<String> getSkillsFor(MonsterType type) {
        return attackSkills.getOrDefault(type, new ArrayList<>());
    }
    
    /**
     * Set monster preference (PREFER, AVOID, NONE).
     */
    public void setMonsterPreference(MonsterType type, MonsterPreference preference) {
        monsterPreferences.put(type, preference);
    }
    
    /**
     * Get monster preference.
     */
    public MonsterPreference getMonsterPreference(MonsterType type) {
        return monsterPreferences.getOrDefault(type, MonsterPreference.NONE);
    }
}
