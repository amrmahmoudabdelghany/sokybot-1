package org.sokybot.machine.controller;

import org.slf4j.Logger;
import org.sokybot.machine.IMachineEvent;
import org.sokybot.machine.MachineState;
import org.sokybot.machine.event.MasteryLvlUpEvent;
import org.sokybot.machine.event.trainerevent.SkillLvlupEvent;
import org.sokybot.machine.event.trainerevent.TrainerLoadedEvent;
import org.sokybot.machine.event.userevent.UserUpdateSkillEvent;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machine.model.ClientFeed;
import org.sokybot.machine.network.PacketListener;
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
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Controller;

@Controller
public class TrainerHandler {

	@Autowired
	private Trainer trainer;

	@Autowired
	private ISroMaterialDAO gameDao;

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private Logger log;

	@Autowired
	private StateMachine<MachineState, IMachineEvent> stateMachine;
	
	@Autowired
	private Settings config ; 

	@PacketListener(opcode = ServerOpcode.CHAR_DATA)
	public void parsingCharData(ImmutablePacket packet) {

		log.info("Character Data : {} ", packet);
		ICharacterDataReader reader = this.ctx.getBean(ICharacterDataReader.class, packet.getStreamReader());

		trainer.setServerTime(reader.getInt());

		int refId = reader.getInt();
		NPCEntity entity = this.ctx.getBean(ISroMaterialDAO.class)
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

		this.ctx.getBean(ISroMaterialDAO.class).findNPC(trainer.getRefId()).ifPresent((npc) -> {
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

	@PacketListener(opcode = ServerOpcode.CHAR_DIE)
	public void onCharDie(ImmutablePacket packet) {

		IStreamReader reader = packet.getStreamReader();

		byte flag = reader.getByte();

		if (flag == 0x04) {

			this.trainer.setLifeState(LifeState.Dead);
		}

	}

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

	@PacketListener(opcode = ClientOpcode.CHAR_SKILL_LVL_UP)
	public void userlvlUpSkill(ImmutablePacket packet) { 
		
		
		int refId  = packet.getStreamReader().getInt() ; 
		
		 this.gameDao
		 .findSkillEntity(refId)
		 .ifPresent((skillEntity)->{
			 
			 	 this.ctx.publishEvent(new UserUpdateSkillEvent(TrainerHandler.this, skillEntity)) ; 
		 });
		 
	}
	@PacketListener(opcode = ServerOpcode.CHAR_SKILL_LVL_UP)
	public void skillLevelUp(ImmutablePacket packet) {
		
		// this code need refactor
		log.info("SkillLvlUp Packet : {} " , packet);
		IStreamReader reader = packet.getStreamReader();
		if (reader.getBoolean()) {
			 
			    
			   int  skillId = reader.getInt() ; 
			   this.gameDao.findSkillEntity(skillId)
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
