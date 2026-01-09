package org.sokybot.machine.controller;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.sokybot.gameevents.events.character.CharacterDeathEvent;
import org.sokybot.gameevents.events.character.CharacterInfoEvent;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.skill.MasteryLevelUpEvent;
import org.sokybot.gameevents.events.skill.SkillLevelUpEvent;
import org.sokybot.gameevents.events.skill.SkillPointsUpdateEvent;
import org.sokybot.gameevents.events.stat.ExpUpdateEvent;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.event.MasteryLvlUpEvent;
import org.sokybot.machine.event.trainerevent.SkillLvlupEvent;
import org.sokybot.machine.event.trainerevent.TrainerLoadedEvent;
import org.sokybot.machine.event.userevent.UserUpdateSkillEvent;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machine.model.ClientFeed;
import org.sokybot.machine.parser.ICharacterDataReader;
import org.sokybot.machinegroup.gamemodel.AttackGainType;
import org.sokybot.machinegroup.gamemodel.item.Item;
import org.sokybot.machinegroup.gamemodel.npc.CharacterStatus;
import org.sokybot.machinegroup.gamemodel.npc.DebuffStatus;
import org.sokybot.machinegroup.gamemodel.npc.FreePVP;
import org.sokybot.machinegroup.gamemodel.npc.JobType;
import org.sokybot.machinegroup.gamemodel.npc.LifeState;
import org.sokybot.machinegroup.gamemodel.npc.MotionState;
import org.sokybot.machinegroup.gamemodel.npc.MovementType;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.machinegroup.gamemodel.npc.NPCType;
import org.sokybot.machinegroup.gamemodel.npc.PVPState;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.gamemodel.skill.Skill;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.gameevents.events.stat.GoldUpdateEvent;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Controller;

@Controller
public class TrainerHandler {

	@Autowired
	private Trainer trainer;

	@Autowired
	private IGameDataLookup gameDao;

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private Logger log;

	@Autowired
	private StateMachine<MachineState, IMachineEvent> stateMachine;
	
	@Autowired
	private Settings config;

	@Autowired
	private BundleContext bundleContext;

	@Value("${" + AppConstants.MACHINE_NAME + "}")
	private String machineName;

	@Value("${" + AppConstants.GROUP_NAME + "}")
	private String groupName;

	private ServiceRegistration<EventHandler> eventHandlerRegistration;

	private String machineFullName() {
		return groupName + "." + machineName;
	}

	@PostConstruct
	public void init() {
		// Register as OSGi EventHandler to listen to game events
		if (bundleContext != null) {
			try {
				Dictionary<String, Object> properties = new Hashtable<>();
				// Subscribe to all game events for this machine
				String machineFullName = machineFullName();
				String topicPattern = "sokybot/game/" + machineFullName + "/*";
				properties.put(EventConstants.EVENT_TOPIC, topicPattern);
				
				// Create EventHandler that delegates to this controller
				EventHandler handler = this::handleGameEvent;
				
				eventHandlerRegistration = bundleContext.registerService(
						EventHandler.class,
						handler,
						properties);
				
				log.info("TrainerHandler registered as OSGi EventHandler for machine: {}", machineFullName);
			} catch (Exception e) {
				log.error("Failed to register TrainerHandler as EventHandler", e);
			}
		}
	}

	@PreDestroy
	public void cleanup() {
		if (eventHandlerRegistration != null) {
			try {
				eventHandlerRegistration.unregister();
				log.info("TrainerHandler EventHandler unregistered");
			} catch (Exception e) {
				log.error("Error unregistering TrainerHandler EventHandler", e);
			}
		}
	}

	/**
	 * Central event handler that routes OSGi events to appropriate methods.
	 */
	private void handleGameEvent(Event osgiEvent) {
		try {
			IGameEvent event = (IGameEvent) osgiEvent.getProperty("event");
			if (event == null) {
				return;
			}

			// Only handle events for this machine
			if (!machineFullName().equals(event.getFullName())) {
				return;
			}

			String eventType = event.getClass().getSimpleName();

			// Route to appropriate handler based on event type
			switch (eventType) {
				case "CharacterLoadedEvent":
					handleCharacterLoadedEvent((CharacterLoadedEvent) event);
					break;
				case "CharacterInfoEvent":
					handleCharacterInfoEvent((CharacterInfoEvent) event);
					break;
				case "ExpUpdateEvent":
					handleExpUpdateEvent((ExpUpdateEvent) event);
					break;
				case "CharacterDeathEvent":
					handleCharacterDeathEvent((CharacterDeathEvent) event);
					break;
				case "GoldUpdateEvent":
					handleGoldUpdateEvent((GoldUpdateEvent) event);
					break;
				case "SkillPointsUpdateEvent":
					handleSkillPointsUpdateEvent((SkillPointsUpdateEvent) event);
					break;
				case "MasteryLevelUpEvent":
					handleMasteryLevelUpEvent((MasteryLevelUpEvent) event);
					break;
				case "SkillLevelUpEvent":
					handleSkillLevelUpEvent((SkillLevelUpEvent) event);
					break;
				default:
					log.debug("Unhandled event type: {}", eventType);
					break;
			}
		} catch (Exception e) {
			log.error("Error handling game event", e);
		}
	}

	// ========== EVENT HANDLERS (migrated from @PacketListener) ==========

	/**
	 * Handles CharacterLoadedEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.CHAR_DATA)
	 * 
	 * NOTE: CharacterLoadedEvent contains basic character data but not the full parsing
	 * of items, masteries, skills, quests that ICharacterDataReader provides.
	 * This handler uses the event data to update trainer with available information.
	 * Full parsing with items/masteries/skills still requires packet parsing for now.
	 */
	private void handleCharacterLoadedEvent(CharacterLoadedEvent event) {
		log.info("Character Loaded Event received");

		// Update basic stats from event
		trainer.setLevel((byte) event.getLevel());
		trainer.setMaxlvl((byte) event.getMaxLevel());
		trainer.setCharEXPOffset(event.getExperience());
		trainer.setGold(event.getGold());
		trainer.setSkillPoint((int) event.getSkillPoints());
		trainer.setCharHP(event.getCurrentHP());
		trainer.setCharMP(event.getCurrentMP());

		// Update position
		trainer.setXSector((byte) event.getXSector());
		trainer.setYSector((byte) event.getYSector());
		trainer.setXOffset(event.getXOffset());
		trainer.setYOffset(event.getYOffset());
		trainer.setZOffset(event.getZOffset());
		trainer.setAngle(event.getAngle());

		// Compute world coordinates
		int x = SilkroadUtils.getXCoord(trainer.getXOffset(), trainer.getXSector(), 10);
		int y = SilkroadUtils.getYCoord(trainer.getYOffset(), trainer.getYSector(), 10);
		trainer.setLocation(x, y);

		// Update speeds
		trainer.setWalkSpeed(event.getWalkSpeed());
		trainer.setRunSpeed(event.getRunSpeed());

		// NOTE: Items, masteries, skills, quests, buffs parsing still requires
		// ICharacterDataReader and packet parsing. The event only contains counts.
		// For now, we trigger the state machine and publish TrainerLoadedEvent.
		// Full parsing can be done later if needed.

		// Trigger state machine transition
		this.stateMachine.sendEvent(ClientFeed.GAME_READY);
		this.ctx.publishEvent(new TrainerLoadedEvent(trainer));

		log.info("Trainer Loaded");
	}

	/**
	 * Handles CharacterInfoEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.CHAR_INFO)
	 * 
	 * NOTE: CharacterInfoEvent doesn't include STR/INT stats that the original code sets.
	 * These are set separately or may need to be added to the event.
	 */
	private void handleCharacterInfoEvent(CharacterInfoEvent event) {
		this.trainer.setPhyAtkMin(event.getPhyAtkMin());
		this.trainer.setPhyAtkMax(event.getPhyAtkMax());
		this.trainer.setMagAtkMin(event.getMagAtkMin());
		this.trainer.setMagAtkMax(event.getMagAtkMax());
		this.trainer.setPhyDef(event.getPhyDef());
		this.trainer.setMagDef(event.getMagDef());
		this.trainer.setHitRate(event.getHitRate());
		this.trainer.setParryRate(event.getParryRate());
		this.trainer.setMaxHP(event.getMaxHP());
		this.trainer.setMaxMP(event.getMaxMP());
		this.trainer.setCharSTR(event.getStrength());
		this.trainer.setCharINT(event.getIntelligence());
	}

	/**
	 * Handles ExpUpdateEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.EXP_SP_UPDATE)
	 */
	private void handleExpUpdateEvent(ExpUpdateEvent event) {
		// Original code computed level changes from exp difference
		// ExpUpdateEvent already contains levelUp flag and total exp
		long newExpOffset = event.getTotalExp();
		long currentExpOffset = this.trainer.getCharEXPOffset();
		long gainedEXP = newExpOffset - currentExpOffset;

		// Handle level changes if levelUp flag is set
		if (event.isLevelUp()) {
			// Level increased - adjust exp offset
			long currentMaxExp = this.gameDao.getLvlEXP(this.trainer.getLevel()).orElse(118L);
			long remainingExp = newExpOffset - currentMaxExp;
			
			// Level might have increased multiple times
			while (remainingExp >= currentMaxExp && this.trainer.getLevel() < 120) {
				this.trainer.setLevel((byte) (this.trainer.getLevel() + 1));
				currentMaxExp = this.gameDao.getLvlEXP(this.trainer.getLevel()).orElse(118L);
				remainingExp -= currentMaxExp;
			}
			
			this.trainer.setCharEXPOffset(remainingExp >= 0 ? remainingExp : newExpOffset);
		} else if (gainedEXP < 0) {
			// Level might have decreased
			long currentMaxExp = this.gameDao.getLvlEXP(this.trainer.getLevel()).orElse(118L);
			long adjustedExp = newExpOffset;
			
			while (adjustedExp < 0 && this.trainer.getLevel() > 1) {
				this.trainer.setLevel((byte) (this.trainer.getLevel() - 1));
				currentMaxExp = this.gameDao.getLvlEXP(this.trainer.getLevel()).orElse(118L);
				adjustedExp += currentMaxExp;
			}
			
			this.trainer.setCharEXPOffset(adjustedExp);
		} else {
			// Normal exp update
			this.trainer.setCharEXPOffset(newExpOffset);
		}
	}

	/**
	 * Handles CharacterDeathEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.CHAR_DIE)
	 */
	private void handleCharacterDeathEvent(CharacterDeathEvent event) {
		// Original code checked a flag byte, but CharacterDeathEvent represents death directly
		this.trainer.setLifeState(LifeState.Dead);
	}

	/**
	 * Handles GoldUpdateEvent from OSGi EventAdmin.
	 * Partially migrated from @PacketListener(opcode = ServerOpcode.ATTACK_GAINS_UPDATE)
	 */
	private void handleGoldUpdateEvent(GoldUpdateEvent event) {
		this.trainer.setGold(event.getNewGoldAmount());
	}

	/**
	 * Handles SkillPointsUpdateEvent from OSGi EventAdmin.
	 * Partially migrated from @PacketListener(opcode = ServerOpcode.ATTACK_GAINS_UPDATE)
	 */
	private void handleSkillPointsUpdateEvent(SkillPointsUpdateEvent event) {
		this.trainer.setSkillPoint(event.getNewSkillPoints());
	}

	/**
	 * Handles MasteryLevelUpEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.CHAR_MASTERY_LVL_UP)
	 */
	private void handleMasteryLevelUpEvent(MasteryLevelUpEvent event) {
		if (event.isSuccess()) {
			int masteryId = event.getMasteryId();
			byte newLevel = (byte) event.getNewLevel();
			
			this.trainer.getMastryList().findMastry(masteryId).ifPresent((m) -> {
				m.setMasteryLevel(newLevel);
				// Publish Spring event for backward compatibility
				this.ctx.publishEvent(new MasteryLvlUpEvent(this, m));
			});
		}
	}

	/**
	 * Handles SkillLevelUpEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.CHAR_SKILL_LVL_UP)
	 */
	private void handleSkillLevelUpEvent(SkillLevelUpEvent event) {
		if (event.isSuccess()) {
			int skillId = event.getSkillId();
			
			this.gameDao.findSkill(skillId).ifPresent((skillEntity) -> {
				Skill skill = new Skill(skillEntity);
				skill.setIsEnabled((byte) 0x01);
				log.info("Skill updated: {}", skill);
				// Publish Spring event for backward compatibility
				this.ctx.publishEvent(new SkillLvlupEvent(skill));
			});
		}
	}

	// ========== DEPRECATED: Still requires packet parsing ==========
	
	/**
	 * DEPRECATED: This handler still requires full packet parsing via ICharacterDataReader
	 * to extract items, masteries, skills, quests, buffs, etc.
	 * CharacterLoadedEvent provides basic data but not full entity parsing.
	 * TODO: Either enhance CharacterLoadedEvent to include full parsing or keep packet parsing for this
	 */
	@Deprecated
	@PacketListener(opcode = ServerOpcode.CHAR_DATA)
	public void parsingCharData(ImmutablePacket packet) {

		log.info("Character Data : {} ", packet);
		ICharacterDataReader reader = this.ctx.getBean(ICharacterDataReader.class, packet.getStreamReader());

		trainer.setServerTime(reader.getInt());

		int refId = reader.getInt();
		NPCEntity entity = this.ctx.getBean(IGameDataLookup.class)
				.findNPC(refId)
				.orElse(NPCEntity.builder().refId(refId).Type(NPCType.UNKNOWN).build());

		trainer.setEntity(entity);

		trainer.setCharScale(reader.getByte());
		trainer.setLevel(reader.getByte());
		trainer.setMaxlvl(reader.getByte());
		trainer.setCharEXPOffset(reader.getLong());
		trainer.setSexpOffSet(reader.getInt());
		trainer.setGold(reader.getLong());
		trainer.setSkillPoint(reader.getInt());
		trainer.setCharStatPoint(reader.getShort());
		trainer.setZerkCount(reader.getByte());
		trainer.setGatheredExpPoint(reader.getInt());
		trainer.setCharHP(reader.getInt());
		trainer.setCharMP(reader.getInt());
		trainer.setAutoInverstExp(reader.getByte());
		trainer.setDailyPK(reader.getByte());
		trainer.setTotalPK(reader.getShort());
		trainer.setPkPenaltyPoint(reader.getInt());
		trainer.setZerkLvl(reader.getByte());
		trainer.setFreePVP(FreePVP.of(reader.getByte()));

		trainer.setItemInventorySize(reader.getByte());
		trainer.setItemCount(reader.getByte());

		this.ctx.getBean(IGameDataLookup.class).findNPC(trainer.getRefId()).ifPresent((npc) -> {
			log.info("Trainer NPC Entity : " + npc);
		});
		// log.info("Trainer : {}" , this.trainer);

		trainer.clearItemInventory();

		for (int i = 0; i < trainer.getItemCount(); i++) {
			trainer.addItem(reader.getItem());
		}

		trainer.setAvaterInventorySize(reader.getByte());
		trainer.setAvaterItemCount(reader.getByte());

		trainer.clearAvaterInventory();

		for (int i = 0; i < trainer.getAvaterItemCount(); i++)
			trainer.addAvaterItem(reader.getItem());

		trainer.setHasMask(reader.getBoolean());

		this.trainer.clearMasteryList();
		while (reader.getBoolean())
			trainer.addMastery(reader.getMastery());

		reader.getByte(); // Mastery end byte

		while (reader.getBoolean()) {

			Skill skill = reader.getSkill();
			trainer.addSkill(skill);
			// log.info("Skill {} ", skill);
		}

		short completedQuest = reader.getShort();
		// log.info("Completed Quest {} ", completedQuest);
		for (int i = 0; i < completedQuest; i++) {
			trainer.addCompletedQuest(reader.getInt());
		}

		byte activeQuest = reader.getByte();
		// log.info("Active Quest {} ", activeQuest);
		for (int i = 0; i < activeQuest; i++)
			trainer.addActiveQuest(reader.getQuest());

		byte unk05 = reader.getByte();

		int collectionBookCount = reader.getInt();
		// log.info("Collection Book Count {} ", collectionBookCount);
		for (int i = 0; i < collectionBookCount; i++) {

			reader.getInt(); // themeIndex
			reader.getInt(); // themeStartedDateTime
			reader.getInt(); // themePages
		}

		trainer.setUniqueId(reader.getInt());
		trainer.setXSector(reader.getUnsignedByte());
		trainer.setYSector(reader.getUnsignedByte());
		trainer.setXOffset(reader.getFloat());
		trainer.setZOffset(reader.getFloat());
		trainer.setYOffset(reader.getFloat());
		trainer.setAngle(SilkroadUtils.getAngle(reader.getShort()));

		// log.info("XOffset : {} , XSector {} " , trainer.getXOffset() ,
		// trainer.getXSector() ) ;
		int x = SilkroadUtils.getXCoord(trainer.getXOffset(), trainer.getXSector(), 10);
		int y = SilkroadUtils.getYCoord(trainer.getYOffset(), trainer.getYSector(), 10);
		// log.info("Trainer Location ({} , {})", x, y);
		trainer.setLocation(x, y);

		trainer.setHasDestination(reader.getBoolean());
		trainer.setMovementType(MovementType.of(reader.getByte()));

		if (trainer.isHasDestination()) {

			trainer.setDestXSector(reader.getUnsignedByte());
			trainer.setDestYSector(reader.getUnsignedByte());

			if (trainer.isInCave()) {

				trainer.setDestXOffset(reader.getInt());
				trainer.setDestZOffset(reader.getInt());
				trainer.setDestYOffset(reader.getInt());
				;
			} else {

				trainer.setDestXOffset(reader.getShort());
				trainer.setDestZOffset(reader.getShort());
				trainer.setDestYOffset(reader.getShort());

			}
			trainer.setDestX(SilkroadUtils.getXCoord(trainer.getXOffset(), trainer.getXSector(), 10));
			trainer.setDestY(SilkroadUtils.getYCoord(trainer.getYOffset(), trainer.getYSector(), 10));

		} else {

			trainer.setSkyClickFlag(reader.getByte());
			trainer.setAngle(SilkroadUtils.getAngle(reader.getShort()));

		}

		trainer.setLifeState(LifeState.of(reader.getByte()));
		trainer.setDebuffStatus(DebuffStatus.of(reader.getByte()));
		trainer.setMotionState(MotionState.of(reader.getByte()));
		trainer.setCharacterStatus(CharacterStatus.of(reader.getByte()));

		trainer.setWalkSpeed(reader.getFloat());
		trainer.setRunSpeed(reader.getFloat());
		trainer.setHwanSpeed(reader.getFloat());

		byte activeBuffCount = reader.getByte();
		// log.info("Active Buff Count : {} ", activeBuffCount);
		for (int i = 0; i < activeBuffCount; i++) {
			trainer.addBuff(reader.getBuff());
		}

		trainer.setName(reader.getString());
		// log.info("Char Name : {} ", trainer.getName());
		trainer.setJobName(reader.getString());
		trainer.setJobType(JobType.of(reader.getByte()));
		trainer.setJobLvl(reader.getByte());
		trainer.setJobExp(reader.getInt());
		trainer.setJobContribution(reader.getInt());
		trainer.setJobReward(reader.getInt());

		trainer.setPvpState(PVPState.of(reader.getByte()));
		trainer.setHasTransport(reader.getBoolean());
		trainer.setInCombat(reader.getBoolean());

		if (trainer.isHasTransport()) {
			trainer.setTransportId(reader.getInt());
		}
		trainer.setPvpFlag(reader.getByte());
		trainer.setGuideFlag(reader.getLong());
		trainer.setAccountId(reader.getInt());
		trainer.setGmFlag(reader.getByte());
		trainer.setActivationFlag(reader.getByte());

		byte hkCounter = reader.getByte();

		for (int i = 0; i < hkCounter; i++)
			trainer.addHotKey(reader.getHotKey());
		;

		trainer.setHpSlot(reader.getByte());
		trainer.setHpValue(reader.getByte());
		trainer.setMpSlot(reader.getByte());
		trainer.setMpValue(reader.getByte());
		trainer.setUniversalSlot(reader.getByte());
		trainer.setUniversalValue(reader.getByte());
		trainer.setPotionDelay(reader.getByte());

		byte blockedPlayerCount = reader.getByte();

		for (int i = 0; i < blockedPlayerCount; i++)
			trainer.addBlockedPlayer(reader.getString());

		trainer.setUnk13(reader.getInt());
		trainer.setUnk14(reader.getByte());

		this.stateMachine.sendEvent(ClientFeed.GAME_READY);
		this.ctx.publishEvent(new TrainerLoadedEvent(trainer));

		log.info("Trainer Loaded");
		;

	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.CHAR_INFO)
	public void parsingCharInfo(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader();
		this.trainer.setPhyAtkMin(reader.getInt());
		this.trainer.setPhyAtkMax(reader.getInt());

		this.trainer.setMagAtkMin(reader.getInt());
		this.trainer.setMagAtkMax(reader.getInt());

		this.trainer.setPhyDef(reader.getShort());
		this.trainer.setMagDef(reader.getShort());
		this.trainer.setHitRate(reader.getShort());
		this.trainer.setParryRate(reader.getShort());
		this.trainer.setMaxHP(reader.getInt());
		this.trainer.setMaxMP(reader.getInt());
		this.trainer.setCharSTR(reader.getShort());
		this.trainer.setCharINT(reader.getShort());
	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.EXP_SP_UPDATE)
	public void expUpdate(ImmutablePacket packet) {

		IStreamReader reader = packet.getStreamReader();

		int monsterId = reader.getInt();

		int gainedEXP = reader.getInt() - reader.getInt();
		int sexp = reader.getInt() - reader.getInt();

		long currentExpOffset = this.trainer.getCharEXPOffset();

		long newExpOffset = currentExpOffset + gainedEXP;

		// log.info("Gained EXP : {} , Max ExpOffset : {} ,Current expOffset : {} ,
		// newExpOffset : {} ", gainedEXP,
		// this.gameDao.getLvlEXP(this.trainer.getLevel()), currentExpOffset,
		// newExpOffset);

		long currentMaxExp = this.gameDao.getLvlEXP(this.trainer.getLevel()).orElse(118L);

		while (newExpOffset < 0) {
			this.trainer.setLevel((byte) (this.trainer.getLevel() - 1));
			currentMaxExp = this.gameDao.getLvlEXP(this.trainer.getLevel()).orElse(118L);
			newExpOffset += currentMaxExp;
			log.info("New EXP Offset Afted Dlvl : {} ", newExpOffset);
			log.info("Max EXP Offset Afted Dlvl : {} ", currentMaxExp);

		}

		while (newExpOffset > currentMaxExp) {

			this.trainer.setLevel((byte) (this.trainer.getLevel() + 1));
			newExpOffset -= currentMaxExp;
			currentMaxExp = this.gameDao.getLvlEXP(this.trainer.getLevel()).get();
		}

		this.trainer.setCharEXPOffset(newExpOffset);

	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.CHAR_DIE)
	public void onCharDie(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader();
		byte flag = reader.getByte();
		if (flag == 0x04) {
			this.trainer.setLifeState(LifeState.Dead);
		}
	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.ATTACK_GAINS_UPDATE)
	public void attackGainsUpdates(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader();

		switch (AttackGainType.of(reader.getByte())) {
		case GOLD:
			this.trainer.setGold(reader.getLong());
			break;
		case ZERK:
			this.trainer.setZerkCount(reader.getByte());
			break;
		case SP:
			this.trainer.setSkillPoint(reader.getInt());
			break;
		case UNKNOWN:
			log.info("Unknown attack gain type detected");
			break;
		}

	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.CHAR_MASTERY_LVL_UP)
	public void masteryLevelUp(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader();
		byte result = reader.getByte();
		if (result == 1) {
			int masteryId = reader.getInt();
			byte newLvl = reader.getByte();
			this.trainer.getMastryList().findMastry(masteryId).ifPresent((m) -> {
				m.setMasteryLevel(newLvl);
				this.ctx.publishEvent(new MasteryLvlUpEvent(this, m));
			});
		}
	}

	@Deprecated
	@PacketListener(opcode = ClientOpcode.CHAR_SKILL_LVL_UP)
	public void userlvlUpSkill(ImmutablePacket packet) { 
		int refId = packet.getStreamReader().getInt();
		this.gameDao.findSkill(refId).ifPresent((skillEntity) -> {
			this.ctx.publishEvent(new UserUpdateSkillEvent(TrainerHandler.this, skillEntity));
		});
	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.CHAR_SKILL_LVL_UP)
	public void skillLevelUp(ImmutablePacket packet) {
		
		// this code need refactor
		log.info("SkillLvlUp Packet : {} " , packet);
		IStreamReader reader = packet.getStreamReader();
		if (reader.getBoolean()) {
			 
			    
			   int  skillId = reader.getInt() ; 
			   this.gameDao.findSkill(skillId)
			   .ifPresent((skillEntity)->{
				   Skill skill =  new Skill(skillEntity) ; 
				   skill.setIsEnabled((byte)0x01); 
				   log.info("Skill updated is : {} " , skill ) ; 
					this.ctx.publishEvent(new SkillLvlupEvent(skill)); 
			   });
		}
	}
	
	
	
	//@PacketListener(opcode = ServerOpcode.SKILL_CAST_CONFIRM)
	public void skillCastConfirm(ImmutablePacket packet) { 
		// action confirmation event 
		// 1-byte OK / added to queue
		// 1-byte Position in Queue 
		
		
		//log.info("Skill Cast Confrim Packet : {} " , packet);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
