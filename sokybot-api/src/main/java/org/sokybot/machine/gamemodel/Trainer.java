/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sokybot.machine.gamemodel;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.sokybot.machine.event.SkillCastStartEvent;
import org.sokybot.machine.event.trainerevent.SkillLvlupEvent;
import org.sokybot.machine.event.trainerevent.TrainerSkillsUpdatedEvent;
import org.sokybot.machine.event.userevent.UserUpdateSkillEvent;
import org.sokybot.machinegroup.gamemodel.npc.FreePVP;
import org.sokybot.machinegroup.gamemodel.npc.HotKey;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.machinegroup.gamemodel.npc.Player;
import org.sokybot.machinegroup.gamemodel.quest.Quest;
import org.sokybot.machinegroup.gamemodel.skill.Mastery;
import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.sokybot.persistence.entities.SkillEntity;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import ch.qos.logback.classic.Logger;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;

/**
 *
 * @author AMROO
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Trainer extends Player {

	public final static String ENTITY_PROPERTY = "entity";
	public final static String CHAR_NAME_PROPERTY = "charName";
	public final static String CHAR_LVL_PROPERTY = "charLvl";
	public final static String GOLD_PROPERTY = "gold";
	public final static String ZERK_COUNT_PROPERTY = "zerkCount";
	public final static String MAX_HP_PROPERTY = "maxHP";
	public final static String MAX_MP_PROPERTY = "maxHM";
	public final static String CURRENT_HP_PROPERTY = "currentHP";
	public final static String CURRENT_MP_PROPERTY = "currentHM";
	public final static String CURRENT_EXP = "currentEXP";
	public final static String MASTERY_ADDED = "masteryAdded";
	// public final static String CURRENT_ZERK_PROPERTY = "currentZerk";
	public final static String SP_PROPERTY = "SP";
	// public final static String XOFFSET_PROPERTY = "xOffset";
	// public final static String YOFFSET_PROPERTY = "yOffset";
	public final static String POSITION_PROPERTY = "position";
	private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

	private int serverTime;
	private byte maxlvl;
	private int sexpOffSet;
	private long gold;
	private int gatheredExpPoint;
	private byte autoInverstExp;
	private int skillPoint;
	private byte gwanCount;
	private byte dailyPK;
	private short totalPK;
	private int pkPenaltyPoint;
	private byte zerkCount;
	private byte zerkLvl;
	private FreePVP freePVP = FreePVP.UNKNOWN;

	private long charEXPOffset; // shared
	private short charSTR; // shared
	private short charINT; // shared
	private short charStatPoint; // shared
	private int charHP; // shared
	private int charMP; // shared
    
    public int getCharHP() { return charHP; }
    public int getCharMP() { return charMP; }

	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	private final Map<Integer , Skill> skillsByID = new HashMap<>();
	

	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	private final Map<String, Skill> skillsByName = new HashMap<>() ; 
	
	
	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	private List<Integer> completedQuests = new ArrayList<>();

	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	private Map<Integer, Quest> activeQuests = new HashMap<>();

	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	private List<HotKey> hotKeys = new ArrayList<>();

	@Setter(value = AccessLevel.NONE)
	private MasteryList mastryList = new MasteryList();
	
	@Autowired
	private ScheduledExecutorService taskExecutor;
 
	@Autowired
	private Logger log ;
	
	private String jobName;

	private int jobExp;

	private int jobContribution;

	private int jobReward;

	private long guideFlag;

	private int accountId;

	private byte gmFlag;

	private byte activationFlag;

	private byte hpSlot;
	private byte hpValue;

	private byte mpSlot;
	private byte mpValue;

	private byte universalSlot;
	private byte universalValue;

	private byte potionDelay;

	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	private Set<String> blockedPlayers;

	private int phyAtkMin;
	private int phyAtkMax;

	private int magAtkMin;
	private int magAtkMax;

	private short phyDef;
	private short magDef;
	private short hitRate;
	private short parryRate;

	private int maxHP;
	private int maxMP;

	// private short STR ;
	// private short INT ;

	private int unk13;

	private byte unk14;

	
	public void clearMasteryList() { 
		this.mastryList.clear(); 
	}
	
	public void clearSkillList() { 
		this.skillsByID.clear();
		this.skillsByName.clear(); 
	}
	
	public Collection<Skill> getSkills() { 
		
		return  Collections.unmodifiableCollection(this.skillsByID.values()) ; 
	}
	
	public void setEntity(NPCEntity entity) {

		NPCEntity oldValue = this.entity;
		this.entity = entity;
		this.changeSupport.firePropertyChange(ENTITY_PROPERTY, oldValue, entity);
	}

	
	public boolean hasSkill(int skillId) { 
		return  this.skillsByID.containsKey(skillId) ;  
	}
	
	public boolean hasSkill(String name) { 
		if(name == null || name.isBlank() ) return false ;  
		return  this.skillsByName.containsKey(name) ; 
	}
	
	private void removeSkill(String withName) { 
 
		Skill skill = this.skillsByName.remove(withName) ; 
		if(skill != null) { 
			this.skillsByID.remove(skill.getRefId()) ; 
		}
		
	}
	
	public void addSkill(Skill skill) {
		Objects.requireNonNull(skill, "Skill object must not null");
		this.skillsByID.put(skill.getRefId() , skill);
		this.skillsByName.put(skill.getName(), skill) ; 
		
	}
	
	public Optional<Skill> findSkill(String name) { 
		return Optional.ofNullable(this.skillsByName.get(name)) ;
	}
	
	public Optional<Skill> findSkill(int refId) { 
		return Optional.ofNullable(this.skillsByID.get(refId)) ;		
	}
	@EventListener
	public void onUserUpdateSkill(UserUpdateSkillEvent event) { 
	  
		// disable this skill until confirmation response 
		SkillEntity entity = event.getSkillEntity() ; 
	   
		// disable this skill
		findSkill(event.getSkillEntity().getName())
		.ifPresent((skill)->{
			skill.setIsEnabled((byte)0x00) ; 
		});
		//this.skillsByID.get(entity.getRefId()).setIsEnabled((byte)0x00);
		
	  
	}
	
	@EventListener
	public void onSkillCastStarted(SkillCastStartEvent event) { 
		
	//	log.info("Trainer Recive event {} where unique id was {} " , event , getUniqueId());
		if(event.getCasterId() == getUniqueId()) {
		 
			
			findSkill(event.getSkillId()) 
			.ifPresent((skill)->{
		 
				skill.setIsEnabled((byte)0x00) ; 
			
				this.taskExecutor.schedule(()->{
					
		//			log.info("Skill {} cooldown end , now it ready to use" , skill.getName()) ;
					skill.setIsEnabled((byte)0x01);
					}, skill.getCooldown(), TimeUnit.MILLISECONDS);
			});
			
		}
		 
		
	}
	@EventListener
	public TrainerSkillsUpdatedEvent onSkillLevelUp(SkillLvlupEvent event) { 
	  
		Skill updatedSkill = event.getSkill() ; 
		// we must consider cool down state
		removeSkill(updatedSkill.getName()); // remove old one
		addSkill(updatedSkill); // add new skill
		
	
		return new TrainerSkillsUpdatedEvent() ; 
	}
	
	//TODO we must handle this event 
	public void onSkillLevelUpFail() { 
		// here we must enable the skill 
	}
	@Override
	public void setName(String charName) {
		String oldValue = getName();
		super.setName(charName);
		this.changeSupport.firePropertyChange(CHAR_NAME_PROPERTY, oldValue, charName);

	}

	@Override
	public void setLevel(byte charLvl) {
		byte old = (byte) getLevel();
		super.setLevel(charLvl);
		this.changeSupport.firePropertyChange(CHAR_LVL_PROPERTY, old, charLvl);
	}

	public void setCharEXPOffset(long charEXPOffset) {
		long old = this.charEXPOffset;
		this.charEXPOffset = charEXPOffset;
		this.changeSupport.firePropertyChange(CURRENT_EXP, old, charEXPOffset);
	}

	public void setGold(long gold) {
		long old = this.gold;
		this.gold = gold;
		this.changeSupport.firePropertyChange(GOLD_PROPERTY, old, gold);
	}

	public void setZerkCount(byte zerkCount) {
		byte old = this.zerkCount;
		this.zerkCount = zerkCount;
		this.changeSupport.firePropertyChange(ZERK_COUNT_PROPERTY, old, zerkCount);
	}

	public void setSkillPoint(int skillPoint) {
		int old = this.skillPoint;
		this.skillPoint = skillPoint;
		this.changeSupport.firePropertyChange(SP_PROPERTY, old, skillPoint);

	}

	public void setCharHP(int charHP) {
		int old = this.charHP;
		this.charHP = charHP;
		this.changeSupport.firePropertyChange(CURRENT_HP_PROPERTY, old, charHP);
	}

	public void setMaxHP(int maxHP) {
		int old = this.maxHP;
		this.maxHP = maxHP;
		this.changeSupport.firePropertyChange(MAX_HP_PROPERTY, old, maxHP);
	}

	public void setCharMP(int charMP) {
		int old = this.charMP;
		this.charMP = charMP;
		this.changeSupport.firePropertyChange(CURRENT_MP_PROPERTY, old, charMP);

	}

	public void setMaxMP(int maxMP) {

		int old = this.maxMP;
		this.maxMP = maxMP;
		this.changeSupport.firePropertyChange(MAX_MP_PROPERTY, old, maxMP);

	}

	@Override
	public void setLocation(int x, int y) {

		Point old = new Point(super.position);

		super.setLocation(x, y);
		this.changeSupport.firePropertyChange(POSITION_PROPERTY, old, new Point(super.position));

	}

	@Override
	public void translate(int x, int y) {
		Point old = new Point(super.position);

		super.translate(x, y);
		this.changeSupport.firePropertyChange(POSITION_PROPERTY, old, new Point(super.position));

	}

	public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
		this.changeSupport.addPropertyChangeListener(property, listener);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
		this.changeSupport.removePropertyChangeListener(property, listener);
	}

	

	public void addBlockedPlayer(String name) {
		Objects.requireNonNull(name, "Blocked player name must not null");

		this.blockedPlayers.add(name);
	}

	public void addMastery(Mastery mastery) {
		Objects.requireNonNull(mastery, "Mastry object must not null");
		this.mastryList.addMastery(mastery);
	}

	public void addCompletedQuest(int refId) {
		this.completedQuests.add(refId);
	}

	public void addActiveQuest(Quest quest) {
		Objects.requireNonNull(quest, "Quest object must not null");
		this.activeQuests.put(quest.getRefID(), quest);

	}

	public void addHotKey(HotKey hotKey) {
		Objects.requireNonNull(hotKey, "Hotkey object must not null");
		this.hotKeys.add(hotKey);
	}

	public Optional<Mastery> findMastry(int id) {
		return this.mastryList.findMastry(id);
	
	}

	public int getHPPercentage() {
		if (this.maxHP == 0)
			return 0;

		return (getCharHP() * 100) / this.maxHP;
	}

	public int getMPPercentage() {
		if (this.maxMP == 0)
			return 0;

		return (getCharMP() * 100) / this.maxMP;
	}

	public int getZerkPercentage() {

		return (zerkCount * 100) / 5;
	}

}
