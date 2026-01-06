package org.sokybot.machine.controller;

import java.awt.Point;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.sokybot.machine.event.DespawnEvent;
import org.sokybot.machine.event.SkillCastErrorEevent;
import org.sokybot.machine.event.SkillCastStartEvent;
import org.sokybot.machine.event.SpawnReachDestinationEvent;
import org.sokybot.machine.event.monsterevent.MonsterDespawnEvent;
import org.sokybot.machine.event.monsterevent.MonsterHPUpdateEvent;
import org.sokybot.machine.event.monsterevent.MonsterSelectedEvent;
import org.sokybot.machine.event.monsterevent.MonsterSpawnEvent;
import org.sokybot.machine.event.trainerevent.TrainerAttackedEvent;
import org.sokybot.machine.event.trainerevent.TrainerReachDestinationEvent;
import org.sokybot.machine.event.trainerevent.TrainerStuckEvent;
import org.sokybot.machine.gamemodel.GameModel;
import org.sokybot.machine.gamemodel.IGameModel;
import org.sokybot.machine.gamemodel.IMutableGameModel;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machine.network.PacketListener;
import org.sokybot.machine.parser.ISpawnParser;
import org.sokybot.machinegroup.gamemodel.HealthChange;
import org.sokybot.machinegroup.gamemodel.item.DropItem;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.machinegroup.gamemodel.npc.IFighter;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.machinegroup.gamemodel.npc.NPCType;
import org.sokybot.machinegroup.gamemodel.npc.Pet;
import org.sokybot.machinegroup.gamemodel.npc.Player;
import org.sokybot.machinegroup.gamemodel.portal.Portal;
import org.sokybot.persistence.entities.PortalEntity;
import org.sokybot.machinegroup.gamemodel.portal.TeleportEntity;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
public class EnvironmentHandler {

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private ISroMaterialDAO sroDao;

	@Autowired
	private IMutableGameModel gameModel;

	@Autowired
	private Trainer trainer;

	@Autowired
	Logger log;

	@Autowired
	private ScheduledExecutorService taskExecutor;

	private Map<Integer, ScheduledFuture<?>> movements = new HashMap<>();

	private Pair<Byte, Short> currentG;

	@PacketListener(opcode = ServerOpcode.GROUP_SPAWN_BEGIN)
	public void onGroupSpawnBegin(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader();
		byte type = reader.getByte();
		short count = reader.getShort();
		this.currentG = Pair.of(type, count);

	}

	@PacketListener(opcode = ServerOpcode.GROUP_SPAWN)
	public void onGroupSpawn(ImmutablePacket packet) {

		if (this.currentG == null)
			throw new IllegalStateException("Could not process group spawn packet because it undefined");

		byte type = currentG.getLeft();
		short count = currentG.getRight();
		System.out.println("On Group Spawn type :  " + type + " count :  " + count);

		try {
			IStreamReader reader = packet.getStreamReader();

			while (count > 0) {

				if (type == 1) {
					System.out.println("On Group spawn  ");
					onSpawn(reader);
				} else if (type == 2) {
					onDespawn(reader);
				}
				count--;
			}
		} catch (IndexOutOfBoundsException ex) {
			log.info(ex.getMessage());
			log.debug("While processing packet {} , expect type is {} , count is {}  an exception happend ", packet,
					type, count);

		}

	}

	@PacketListener(opcode = ServerOpcode.GROUP_SPAWN_END)
	public void onGroupSpawnEnd(ImmutablePacket packet) {
		this.currentG = null;
	}

	@PacketListener(opcode = ServerOpcode.SINGLE_SPAWN)
	public void onSingleSpawn(ImmutablePacket packet) {
		;
		onSpawn(packet.getStreamReader());
	}

	@PacketListener(opcode = ServerOpcode.SINGLE_DESPAWN)
	public void onSingleDespawn(ImmutablePacket packet) {
		onDespawn(packet.getStreamReader());

	}

	@PacketListener(opcode = ServerOpcode.SKILL_CAST_STARTED)
	public void onSkillCastStarted(ImmutablePacket packet) {

		// log.info("Skill Cast Started Packet {} " , packet);

		IStreamReader reader = packet.getStreamReader();

		if (reader.getBoolean()) { // if success

			reader.getShort();

			int skillId = reader.getInt();

			int casterId = reader.getInt();
			reader.getInt(); // unknow
			int targetId = reader.getInt();

			this.ctx.publishEvent(
					SkillCastStartEvent.builder().skillId(skillId).casterId(casterId).targetId(targetId).build());

			if (targetId == this.trainer.getUniqueId()) {
				this.ctx.publishEvent(TrainerAttackedEvent.builder().casterId(casterId).skillId(skillId).build());
			}

		} else {

			this.ctx.publishEvent(new SkillCastErrorEevent(packet));
		}

	}

	@PacketListener(opcode = ServerOpcode.SKILL_CAST_ENDED)
	public void onSkillCastEnd(ImmutablePacket packet) {

	}

	private void onDespawn(IStreamReader reader) {

		int uniqueId = reader.getInt();

		this.gameModel.remove(uniqueId);

		// this.gameModel.find(uniqueId, Monster.class)
		// .ifPresentOrElse((m)->{
		// this.ctx.publishEvent(new MonsterDespawnEvent(EnvironmentHandler.this, m)) ;
		// this.gameModel.remove(m.getUniqueId()) ;
		// }, ()->this.ctx.publishEvent(new DespawnEvent(this, uniqueId)));

//		this.gameModel.remove(refId);

		Optional.ofNullable(this.movements.remove(uniqueId)).ifPresent((movment) -> movment.cancel(true));

	}

	private void onSpawn(IStreamReader reader) {

		int refId = reader.getInt();

		if (refId == 4294967295l) {

			System.out.println("Special ref id found " + 4294967295l);
		} else if (refId == 4294967294l) {
			System.out.println("Special ref id found " + 4294967294l);
		}

		ISpawnParser spawnParser = this.ctx.getBean(ISpawnParser.class, reader);

		AtomicBoolean processed = new AtomicBoolean(false);
		// item entity
		this.sroDao.findItemEntity(refId).ifPresent((itemEntity) -> {

			DropItem dropItem = spawnParser.readDropItem(itemEntity);
			gameModel.add(dropItem);
			System.out.println("Found Item Entity ") ; 
			processed.set(true);
		});

		if (processed.get()) {
			return;
		}

		// portal entity
		this.sroDao.findPortal(refId).ifPresent((portalEntity) -> {
			// process portal entity
			Portal portal = spawnParser.readPortal(portalEntity);
			gameModel.add(portal);
			System.out.println("Found Portal Entity") ; 
			processed.set(true);

		});

		if (processed.get()) {
			return;
		}

		// teleport
		this.sroDao.findTeleport(refId).ifPresent((teleport) -> {
		
				System.out.println("Detect Teleport entity ") ; 
			processed.set(true) ; 
			
		});

		if (processed.get()) {
			return;
		}

		
		
		this.sroDao.findShop(refId).ifPresent((shop) -> {
			System.out.println("Detect Shop entity ") ; 
			processed.set(true) ; 
			
		});

		if (processed.get()) {
			return;
		}

		System.out.println("On Spawn : " + refId);
		this.sroDao.findNPC(refId).ifPresent((npcEntity) -> {
			NPCType type = npcEntity.getType();
			String longId = npcEntity.getLongId();

			if (type == NPCType.PlayerCh || type == NPCType.PlayerEU) {

				Player player = spawnParser.readPlayer(npcEntity);
				gameModel.add(player);
				// log.info("Player Detected : {} " , player) ;
			} else if (longId.contains("MOB_")) {

				Monster monster = spawnParser.readMonster(npcEntity);

				gameModel.add(monster);
				// System.out.println("Monster " + monster.getRefId() + " Added to gamemodel" )
				// ;
				// this.ctx.publishEvent(new MonsterSpawnEvent(EnvironmentHandler.this,
				// monster));

			} else if (longId.contains("COS_")) {

				Pet pet = spawnParser.readPet(npcEntity);
				gameModel.add(pet);
				log.debug("Detected Pet : {} ", pet);
			} else {
				log.debug("NPC Entity Detected : {} ", npcEntity);
				log.debug("There exists unknown NPC spawn type");
				throw new IllegalStateException("unknown  NPC Entity with id " + refId + " " +  longId);
				// gameModel.add(npcEntity) ;
			}
		});

//		this.sroDao.findEntity(refId).ifPresentOrElse((entity) -> {
//			 log.debug("Spwan entity : " + entity) ;
//			ISpawnParser spawnParser = this.ctx.getBean(ISpawnParser.class, reader);
//			if (entity instanceof ItemEntity) {
//
//				DropItem item = spawnParser.readDropItem(((ItemEntity) entity));
//
//				gameModel.add(item);
//
//				 log.debug("Drop Item Detected : {} " ,item ) ;
//
//			} else if (entity instanceof PortalEntity) {
//
//				Portal portal = spawnParser.readPortal((PortalEntity) entity);
//				gameModel.add(portal);
//				 log.info("Portal Detected : {} ", portal);
//
//			} else if (entity instanceof TeleportEntity) {
//				 log.debug("Teleport Entity Detected : {} " , ((TeleportEntity) entity)) ;
//				 log.debug("On Detect Teleport Entity") ;
//				log.debug("There exists unknown spawn type");
//
//			} else if (entity instanceof NPCEntity) {
//				NPCEntity npcEntity = (NPCEntity) entity;
//				NPCType type = npcEntity.getType();
//				String longId = npcEntity.getLongId();
//
//				if (type == NPCType.PlayerCh || type == NPCType.PlayerEU) {
//
//					Player player = spawnParser.readPlayer(npcEntity);
//					gameModel.add(player);
//					// log.info("Player Detected : {} " , player) ;
//				} else if (longId.contains("MOB_")) {
//
//					Monster monster = spawnParser.readMonster(npcEntity);
//					
//					this.ctx.publishEvent(new MonsterSpawnEvent(EnvironmentHandler.this, monster)) ; 
//					
//					//gameModel.add(monster);
//					
//					
//				} else if (longId.contains("COS_")) {
//
//					Pet pet = spawnParser.readPet(npcEntity);
//					gameModel.add(pet);
//					 log.debug("Detected Pet : {} " , pet ) ;
//				} else {
//					 log.debug("NPC Entity Detected : {} ", ((NPCEntity) entity));
//					log.debug("There exists unknown NPC spawn type");
//				}
//			} else {
//				 log.debug("Entity Detected : {}", entity);
//				log.debug("There exists unknown spawn type");
//			}
//
//		} , ()->{
//			log.debug("Could not find entity with refId {} " , refId);
//			
//		});

	}

	@PacketListener(opcode = ServerOpcode.HPMP_UPDATE)
	public void onHPMPUpdate(ImmutablePacket packet) {

		IStreamReader reader = packet.getStreamReader();

		int id = reader.getInt();
		reader.getShort();
		HealthChange changeType = HealthChange.of(reader.getByte());
		gameModel.find(id, IFighter.class).ifPresent((target) -> {

			if (target instanceof Trainer) {
				Trainer trainer = (Trainer) target;
				switch (changeType) {
				case HPChanged:
					trainer.setCharHP(reader.getInt());
					break;
				case MPChanged:
					trainer.setCharMP(reader.getInt());
					break;
				case HPAndMPChanged:
					trainer.setCharHP(reader.getInt());
					trainer.setCharMP(reader.getInt());
					break;
				case BadStatus:
					int badStatus = reader.getInt(); // 0 = stop, 1 = fire (?), 2 = ice, 3 = freeze, 4 = electricity, 8
														// = fire, 16 = poison
					log.debug("Bad Status Value : {} ", badStatus);
					break;
				case HPAndBadStatusOrMonster:
					trainer.setCharHP(reader.getInt());
					int _badStatus = reader.getInt(); // 0 = stop, 1 = fire (?), 2 = ice, 3 = freeze, 4 = electricity, 8
														// = fire, 16 = poison
					log.debug("Bad Status Value : {} ", _badStatus);
					break;
				case MPAndBadStatus:
					trainer.setCharMP(reader.getInt());
					int __badStatus = reader.getInt(); // 0 = stop, 1 = fire (?), 2 = ice, 3 = freeze, 4 = electricity,
														// 8
					// = fire, 16 = poison
					log.debug("Bad Status Value : {} ", __badStatus);
					break;
				}
			} else if (target instanceof Monster) {
				if (changeType == HealthChange.HPAndBadStatusOrMonster) {
					int currentHP = reader.getInt();
					target.setCurrentHP(currentHP);
					// log.info("Monster With id {} HP changed to {} " , id , target.getCurrentHP())
					// ;
					this.ctx.publishEvent(new MonsterHPUpdateEvent((Monster) target, target.getUniqueId(), currentHP));
				}
			} else if (target instanceof Pet) {

			}
		});

	}

	@PacketListener(opcode = ServerOpcode.SPEED_UPDATE)
	public void speedUpdate(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader();

		this.gameModel.find(reader.getInt(), Player.class).ifPresent((player) -> {
			player.setWalkSpeed(reader.getFloat());
			player.setRunSpeed(reader.getFloat());

		});

	}

	@PacketListener(opcode = ServerOpcode.ANGLE_UPDATE)
	public void onAngleChanged(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader();
		this.gameModel.find(reader.getInt(), Trainer.class).ifPresent((trainer) -> {
			trainer.setAngle(SilkroadUtils.getAngle(reader.getShort()));
			// log.info("New Angle is : {} ", trainer.getAngle());
		});
		;
	}

	@PacketListener(opcode = ServerOpcode.SPAWN_SELECTED)
	public void onSpawnSelected(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader();

		if (reader.getBoolean()) {

			int selectedId = reader.getInt();

			this.gameModel.setSelectedSpawn(selectedId);

			if (reader.getBoolean()) {
				this.gameModel.find(selectedId, IFighter.class).ifPresent((f) -> {

					f.setCurrentHP(reader.getInt());

					if (f instanceof Monster) {
						this.ctx.publishEvent(new MonsterSelectedEvent((Monster) f));
					}
				});

			}
		}

	}

	@PacketListener(opcode = ServerOpcode.SPAWN_MOVEMENT)
	public void onSpawnMove(ImmutablePacket packet) {

		IStreamReader reader = packet.getStreamReader();

		this.gameModel.find(reader.getInt(), IFighter.class).ifPresent((fighter) -> {

			fighter.setHasDestination(reader.getBoolean());

			if (fighter.isHasDestination()) {

				fighter.setDestXSector(reader.getUnsignedByte());
				fighter.setDestYSector(reader.getUnsignedByte());

				if (fighter.isInCave()) {

					fighter.setDestXOffset(reader.getInt());
					fighter.setDestZOffset(reader.getInt());
					fighter.setDestYOffset(reader.getInt());

				} else {

					fighter.setDestXOffset(reader.getShort());
					fighter.setDestZOffset(reader.getShort());
					fighter.setDestYOffset(reader.getShort());

				}

				fighter.setDestX(SilkroadUtils.getXCoord(fighter.getDestXOffset(), fighter.getDestXSector()));
				fighter.setDestY(SilkroadUtils.getYCoord(fighter.getDestYOffset(), fighter.getDestYSector()));
				// log.info("Char Has Destination ({} , {})" , fighter.getDestX() ,
				// fighter.getDestY());

			} else {
				fighter.setSkyClickFlag(reader.getByte());
				// log.info("Server Say That SkyClickFlag {} ", fighter.getSkyClickFlag());
				byte angleAction = reader.getByte();

				// log.info("Char Does`nt have distnation and Sky Flag is {} , angle is {} " ,
				// fighter.getSkyClickFlag() , angleAction) ;

				if (fighter.getSkyClickFlag() == 1) {

					fighter.setAngle(angleAction);
				}
				// byte angleAction = reader.getByte() ; // 0 absolute , 1 go forward
				// fighter.setAngle(reader.getByte()); // 0 absolute , 1 go forward
				// log.info("No Dest , Angle Action is : {} , Sky Flag : {} ", angleAction,
				// fighter.getSkyClickFlag());
			}

			if (reader.getBoolean()) { // has Origin
				// log.info("Char has origin");
				fighter.setXSector(reader.getUnsignedByte());
				fighter.setYSector(reader.getUnsignedByte());
				fighter.setXOffset(reader.getShort());
				fighter.setZOffset(reader.getShort());

				fighter.setAngle(SilkroadUtils.getAngle(reader.getShort()));
				fighter.setYOffset(reader.getShort());
				// log.info("Has Origin And Angle is : {} ", fighter.getAngle());

				fighter.setLocation(SilkroadUtils.getXCoord(fighter.getXOffset(), fighter.getXSector(), 100),
						SilkroadUtils.getYCoord(fighter.getYOffset(), fighter.getYSector(), 100));

			}
			translate(fighter);
		});
	}

	@PacketListener(opcode = ServerOpcode.SPAWN_STUCK)
	public void onStopMovement(ImmutablePacket packet) {
		log.info("On Stop Movement");
		IStreamReader reader = packet.getStreamReader();

		int uniqueId = reader.getInt();
		ScheduledFuture<?> movement = this.movements.get(uniqueId);
		if (movement != null) {
			movement.cancel(true);
		}
		this.gameModel.find(uniqueId).ifPresent((spawn) -> {
			if (spawn instanceof Trainer) {
				log.info("Trainer stop movement");
				TrainerStuckEvent event = new TrainerStuckEvent(this, packet);
				Trainer trainer = this.gameModel.getTrainer();

				trainer.setXSector(event.getXSector());
				trainer.setYSector(event.getYSector());
				trainer.setXOffset(event.getX());
				trainer.setYOffset(event.getY());
				trainer.setZOffset(event.getZ());
				Point location = event.getLocation();
				trainer.setLocation((int) location.getX(), (int) location.getY());
				trainer.setAngle(event.getAngle());

				this.ctx.publishEvent(event);

			} else if (spawn instanceof Monster) {
				log.info("Monster stop movement");
			}
		});

	}

	private void translate(final IFighter fighter) {

		ScheduledFuture<?> movement = movements.remove(fighter.getUniqueId());
		if (movement != null) {
			movement.cancel(true);
		}

		movement = this.taskExecutor.scheduleAtFixedRate(() -> {

			double angle = 0; // radians

			if (fighter.isHasDestination()) {
				angle = Math.atan2(fighter.getDestY() - fighter.getY(), fighter.getDestX() - fighter.getX());
				// log.info("Char has dest , angle is : {} , skyflag : {} ", angle,
				// fighter.getSkyClickFlag());
			} else {
				angle = Math.toRadians(fighter.getAngle());

				// log.info("Char Doesn`t have dest , angle is : {} , skyflag : {} ", angle,
				// fighter.getSkyClickFlag());

				/**
				 * here we must check if arrow keys is pressed then we must
				 */
			}

			if (angle != 0) {

				fighter.translate((int) Math.round(Math.cos(angle)), (int) Math.round(Math.sin(angle)));

			} else {
				// log.info("Reatched to its destination ");

				this.movements.remove(fighter.getUniqueId()).cancel(true);

				if (fighter.getUniqueId() == this.trainer.getUniqueId()) {
					this.ctx.publishEvent(new TrainerReachDestinationEvent(EnvironmentHandler.this, trainer,
							fighter.getDestX(), fighter.getDestY()));
				} else {
					this.ctx.publishEvent(new SpawnReachDestinationEvent(EnvironmentHandler.this, fighter.getUniqueId(),
							fighter.getX(), fighter.getY()));
				}
			}

		}, 0, (long) (1000 / (fighter.getRunSpeed() * 0.1)), TimeUnit.MILLISECONDS);

		this.movements.put(fighter.getUniqueId(), movement);

	}

}
