package org.sokybot.behaviors.training.api;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.sokybot.gameevents.enums.MonsterType;
import org.sokybot.settings.MonsterPreference;

public class TrainingSettings {
    private boolean autoAttack = true;
    private boolean doNotAttack;
    private boolean iterateSkillsPerMonster = true;
    private boolean preferAttackerMonster = true;

    private final Map<MonsterType, List<String>> attackSkills = new EnumMap<MonsterType, List<String>>(MonsterType.class);
    private final Map<MonsterType, MonsterPreference> monsterPreferences = new EnumMap<MonsterType, MonsterPreference>(MonsterType.class);

    private int hpPotionThreshold = 50;
    private int mpPotionThreshold = 50;
    private int hpPetPotionThreshold = 50;
    private boolean useHpPotion = true;
    private boolean useMpPotion = true;
    private boolean usePetPotion = true;

    private boolean loopInTown = true;
    private String scriptPath = "";
    private boolean reverseReturnScroll;

    private String activeAreaName = "";
    private int areaX;
    private int areaY;
    private int areaRadius;

    public boolean isAutoAttack() { return autoAttack; }
    public void setAutoAttack(boolean autoAttack) { this.autoAttack = autoAttack; }
    public boolean isDoNotAttack() { return doNotAttack; }
    public void setDoNotAttack(boolean doNotAttack) { this.doNotAttack = doNotAttack; }
    public boolean isIterateSkillsPerMonster() { return iterateSkillsPerMonster; }
    public void setIterateSkillsPerMonster(boolean iterateSkillsPerMonster) { this.iterateSkillsPerMonster = iterateSkillsPerMonster; }
    public boolean isPreferAttackerMonster() { return preferAttackerMonster; }
    public void setPreferAttackerMonster(boolean preferAttackerMonster) { this.preferAttackerMonster = preferAttackerMonster; }
    public Map<MonsterType, List<String>> getAttackSkills() { return attackSkills; }
    public Map<MonsterType, MonsterPreference> getMonsterPreferences() { return monsterPreferences; }
    public int getHpPotionThreshold() { return hpPotionThreshold; }
    public void setHpPotionThreshold(int hpPotionThreshold) { this.hpPotionThreshold = hpPotionThreshold; }
    public int getMpPotionThreshold() { return mpPotionThreshold; }
    public void setMpPotionThreshold(int mpPotionThreshold) { this.mpPotionThreshold = mpPotionThreshold; }
    public int getHpPetPotionThreshold() { return hpPetPotionThreshold; }
    public void setHpPetPotionThreshold(int hpPetPotionThreshold) { this.hpPetPotionThreshold = hpPetPotionThreshold; }
    public boolean isUseHpPotion() { return useHpPotion; }
    public void setUseHpPotion(boolean useHpPotion) { this.useHpPotion = useHpPotion; }
    public boolean isUseMpPotion() { return useMpPotion; }
    public void setUseMpPotion(boolean useMpPotion) { this.useMpPotion = useMpPotion; }
    public boolean isUsePetPotion() { return usePetPotion; }
    public void setUsePetPotion(boolean usePetPotion) { this.usePetPotion = usePetPotion; }
    public boolean isLoopInTown() { return loopInTown; }
    public void setLoopInTown(boolean loopInTown) { this.loopInTown = loopInTown; }
    public String getScriptPath() { return scriptPath; }
    public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }
    public boolean isReverseReturnScroll() { return reverseReturnScroll; }
    public void setReverseReturnScroll(boolean reverseReturnScroll) { this.reverseReturnScroll = reverseReturnScroll; }
    public String getActiveAreaName() { return activeAreaName; }
    public void setActiveAreaName(String activeAreaName) { this.activeAreaName = activeAreaName; }
    public int getAreaX() { return areaX; }
    public void setAreaX(int areaX) { this.areaX = areaX; }
    public int getAreaY() { return areaY; }
    public void setAreaY(int areaY) { this.areaY = areaY; }
    public int getAreaRadius() { return areaRadius; }
    public void setAreaRadius(int areaRadius) { this.areaRadius = areaRadius; }

    public void addSkill(MonsterType type, String skill) {
        List<String> list = attackSkills.computeIfAbsent(type, key -> new ArrayList<String>());
        list.add(skill);
    }

    public List<String> getSkillsFor(MonsterType type) {
        return attackSkills.getOrDefault(type, new ArrayList<String>());
    }

    public void setMonsterPreference(MonsterType type, MonsterPreference preference) {
        monsterPreferences.put(type, preference);
    }

    public MonsterPreference getMonsterPreference(MonsterType type) {
        return monsterPreferences.getOrDefault(type, MonsterPreference.NONE);
    }
}
