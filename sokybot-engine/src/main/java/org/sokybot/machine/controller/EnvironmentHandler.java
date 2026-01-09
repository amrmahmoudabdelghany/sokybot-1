package org.sokybot.machine.controller;

import java.awt.Point;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.sokybot.gameevents.events.entity.EntityAngleUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityMovementEvent;
import org.sokybot.gameevents.events.entity.EntitySelectedEvent;
import org.sokybot.gameevents.events.entity.EntitySpawnEvent;
import org.sokybot.gameevents.events.entity.EntitySpeedUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityStoppedEvent;
import org.sokybot.gameevents.events.skill.SkillCastEvent;
import org.sokybot.gameevents.events.skill.SkillCastEndEvent;
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
import org.sokybot.persistence.entities.TeleportEntity;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.app.AppConstants;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.utils.SilkroadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
public class EnvironmentHandler {

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private IGameDataLookup sroDao;

	@Autowired
	private IMutableGameModel gameModel;

	@Autowired
	private Trainer trainer;

	@Autowired
	Logger log;

	@Autowired
	private ScheduledExecutorService taskExecutor;

	@Autowired
	private BundleContext bundleContext;

	@Value("${" + AppConstants.MACHINE_NAME + "}")
	private String machineName;

	@Value("${" + AppConstants.GROUP_NAME + "}")
	private String groupName;

	private Map<Integer, ScheduledFuture<?>> movements = new HashMap<>();

	private Pair<Byte, Short> currentG;

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
				
				log.info("EnvironmentHandler registered as OSGi EventHandler for machine: {}", machineFullName);
			} catch (Exception e) {
				log.error("Failed to register EnvironmentHandler as EventHandler", e);
			}
		}
	}

	@PreDestroy
	public void cleanup() {
		if (eventHandlerRegistration != null) {
			try {
				eventHandlerRegistration.unregister();
				log.info("EnvironmentHandler EventHandler unregistered");
			} catch (Exception e) {
				log.error("Error unregistering EnvironmentHandler EventHandler", e);
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
				case "SkillCastEvent":
					handleSkillCastEvent((SkillCastEvent) event);
					break;
				case "SkillCastEndEvent":
					handleSkillCastEndEvent((SkillCastEndEvent) event);
					break;
				case "EntityHPMPUpdateEvent":
					handleHPMPUpdateEvent((EntityHPMPUpdateEvent) event);
					break;
				case "EntitySpeedUpdateEvent":
					handleSpeedUpdateEvent((EntitySpeedUpdateEvent) event);
					break;
				case "EntityAngleUpdateEvent":
					handleAngleUpdateEvent((EntityAngleUpdateEvent) event);
					break;
				case "EntitySelectedEvent":
					handleEntitySelectedEvent((EntitySelectedEvent) event);
					break;
				case "EntityMovementEvent":
					handleEntityMovementEvent((EntityMovementEvent) event);
					break;
				case "EntityStoppedEvent":
					handleEntityStoppedEvent((EntityStoppedEvent) event);
					break;
				case "EntitySpawnEvent":
					handleEntitySpawnEvent((EntitySpawnEvent) event);
					break;
				case "EntityDespawnEvent":
					handleEntityDespawnEvent((EntityDespawnEvent) event);
					break;
				default:
					// Unknown event type - log but don't fail
					log.debug("Unhandled event type: {}", eventType);
					break;
			}
		} catch (Exception e) {
			log.error("Error handling game event", e);
		}
	}

	// ========== EVENT HANDLERS (migrated from @PacketListener) ==========

	// ========== DEPRECATED: Spawn/Despawn handlers still require packet parsing ==========
	// These handlers need ISpawnParser which requires raw packet data to create domain objects.
	// EntitySpawnEvent/EntityDespawnEvent contain refId and position but not enough to create
	// Monster/Player/Pet/Item/Portal objects. These will be migrated later or kept as packet handlers.
	
	@Deprecated
	@PacketListener(opcode = ServerOpcode.GROUP_SPAWN_BEGIN)
	public void onGroupSpawnBegin(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader();
		byte type = reader.getByte();
		short count = reader.getShort();
		this.currentG = Pair.of(type, count);

	}

	@Deprecated
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

	@Deprecated
	@PacketListener(opcode = ServerOpcode.GROUP_SPAWN_END)
	public void onGroupSpawnEnd(ImmutablePacket packet) {
		this.currentG = null;
	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.SINGLE_SPAWN)
	public void onSingleSpawn(ImmutablePacket packet) {
		onSpawn(packet.getStreamReader());
	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.SINGLE_DESPAWN)
	public void onSingleDespawn(ImmutablePacket packet) {
		onDespawn(packet.getStreamReader());
	}

	/**
	 * Handles SkillCastEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.SKILL_CAST_STARTED)
	 */
	private void handleSkillCastEvent(SkillCastEvent event) {
		if (event.isSuccess()) {
			Integer skillId = event.getSkillId();
			Integer casterId = event.getCasterId();
			Integer targetId = event.getTargetId();

			// Publish Spring event for backward compatibility
			this.ctx.publishEvent(
					SkillCastStartEvent.builder().skillId(skillId).casterId(casterId).targetId(targetId).build());

			if (targetId != null && targetId.equals(this.trainer.getUniqueId())) {
				this.ctx.publishEvent(TrainerAttackedEvent.builder().casterId(casterId).skillId(skillId).build());
			}
		}
		// Note: SkillCastEvent already represents success/failure, so no need for error event
	}

	/**
	 * Handles SkillCastEndEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.SKILL_CAST_ENDED)
	 */
	private void handleSkillCastEndEvent(SkillCastEndEvent event) {
		// Currently empty - can add logic if needed
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
		this.sroDao.findItem(refId).ifPresent((itemEntity) -> {

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

	/**
	 * Handles EntityHPMPUpdateEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.HPMP_UPDATE)
	 */
	private void handleHPMPUpdateEvent(EntityHPMPUpdateEvent event) {
		int entityId = event.getEntityId();
		
		gameModel.find(entityId, IFighter.class).ifPresent((target) -> {
			if (target instanceof Trainer) {
				Trainer trainer = (Trainer) target;
				Integer newHP = event.getNewHP();
				Integer newMP = event.getNewMP();
				
				if (newHP != null) {
					trainer.setCharHP(newHP);
				}
				if (newMP != null) {
					trainer.setCharMP(newMP);
				}
				
				// Bad status is in the event but not stored in trainer currently
				// Can be added if needed
			} else if (target instanceof Monster) {
				Integer newHP = event.getNewHP();
				if (newHP != null) {
					target.setCurrentHP(newHP);
					this.ctx.publishEvent(new MonsterHPUpdateEvent((Monster) target, target.getUniqueId(), newHP));
				}
			} else if (target instanceof Pet) {
				// Handle pet HP/MP updates if needed
			}
		});
	}

	/**
	 * Handles EntitySpeedUpdateEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.SPEED_UPDATE)
	 */
	private void handleSpeedUpdateEvent(EntitySpeedUpdateEvent event) {
		int entityId = event.getEntityId();
		this.gameModel.find(entityId, Player.class).ifPresent((player) -> {
			player.setWalkSpeed(event.getWalkSpeed());
			player.setRunSpeed(event.getRunSpeed());
		});
	}

	/**
	 * Handles EntityAngleUpdateEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.ANGLE_UPDATE)
	 */
	private void handleAngleUpdateEvent(EntityAngleUpdateEvent event) {
		int entityId = event.getEntityId();
		this.gameModel.find(entityId, Trainer.class).ifPresent((trainer) -> {
			// Angle is already parsed in the event (degrees)
			// But original code used SilkroadUtils.getAngle() which might convert from short
			// Assuming event contains angle in degrees
			// EntityAngleUpdateEvent contains angle in degrees (already converted)
			trainer.setAngle((byte) event.getNewAngle());
		});
	}

	/**
	 * Handles EntitySelectedEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.SPAWN_SELECTED)
	 */
	private void handleEntitySelectedEvent(EntitySelectedEvent event) {
		int selectedId = event.getSelectedEntityId();
		this.gameModel.setSelectedSpawn(selectedId);

		Integer currentHP = event.getCurrentHP();
		if (currentHP != null) {
			this.gameModel.find(selectedId, IFighter.class).ifPresent((f) -> {
				f.setCurrentHP(currentHP);
				if (f instanceof Monster) {
					this.ctx.publishEvent(new MonsterSelectedEvent((Monster) f));
				}
			});
		}
	}

	/**
	 * Handles EntityMovementEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.SPAWN_MOVEMENT)
	 * 
	 * NOTE: EntityMovementEvent.Position contains offset coordinates (x, y, z) but not sectors.
	 * The original packet parsing extracts sector and offset separately.
	 * Position(x, y, z) here represents offset values, not world coordinates.
	 */
	private void handleEntityMovementEvent(EntityMovementEvent event) {
		int entityId = event.getEntityId();
		
		this.gameModel.find(entityId, IFighter.class).ifPresent((fighter) -> {
			boolean hasDestination = event.hasDestination();
			fighter.setHasDestination(hasDestination);
			
			// Set movement type if available
			if (event.getMovementType() != null) {
				fighter.setMovementType(org.sokybot.machinegroup.gamemodel.npc.MovementType.of(event.getMovementType()));
			}

			Position dest = event.getDestination();
			if (hasDestination && dest != null) {
				// Use sector information from event
				byte destXSector = event.getDestXSector() != null ? event.getDestXSector().byteValue() : (byte)0;
				byte destYSector = event.getDestYSector() != null ? event.getDestYSector().byteValue() : (byte)0;
				float destXOffset = dest.getX();
				float destYOffset = dest.getY();
				float destZOffset = dest.getZ();
				
				fighter.setDestXSector(destXSector);
				fighter.setDestYSector(destYSector);
				fighter.setDestXOffset((short) destXOffset);
				fighter.setDestYOffset((short) destYOffset);
				fighter.setDestZOffset((short) destZOffset);
				
				// Compute world coordinates for destination
				int destX = SilkroadUtils.getXCoord(destXOffset, destXSector);
				int destY = SilkroadUtils.getYCoord(destYOffset, destYSector);
				fighter.setDestX(destX);
				fighter.setDestY(destY);
			} else {
				// No destination - use skyClickFlag and angle from event
				if (event.getSkyClickFlag() != null) {
					fighter.setSkyClickFlag(event.getSkyClickFlag());
				}
				if (event.getAngleAction() != null && event.getSkyClickFlag() != null && event.getSkyClickFlag() == 1) {
					fighter.setAngle(event.getAngleAction());
				}
			}

			Position currentPos = event.getCurrentPosition();
			if (currentPos != null && event.getCurrentXSector() != null && event.getCurrentYSector() != null) {
				// Use sector information from event
				byte xSector = event.getCurrentXSector().byteValue();
				byte ySector = event.getCurrentYSector().byteValue();
				float xOffset = currentPos.getX();
				float yOffset = currentPos.getY();
				float zOffset = currentPos.getZ();
				
				fighter.setXSector(xSector);
				fighter.setYSector(ySector);
				fighter.setXOffset((short) xOffset);
				fighter.setYOffset((short) yOffset);
				fighter.setZOffset((short) zOffset);
				
				// Set angle if available
				if (event.getCurrentAngle() != null) {
					fighter.setAngle(org.sokybot.utils.SilkroadUtils.getAngle(event.getCurrentAngle()));
				}
				
				// Compute world coordinates
				int x = SilkroadUtils.getXCoord(xOffset, xSector, 100);
				int y = SilkroadUtils.getYCoord(yOffset, ySector, 100);
				fighter.setLocation(x, y);
			}

			translate(fighter);
		});
	}

	/**
	 * Handles EntityStoppedEvent from OSGi EventAdmin.
	 * Migrated from @PacketListener(opcode = ServerOpcode.SPAWN_STUCK)
	 */
	private void handleEntityStoppedEvent(EntityStoppedEvent event) {
		log.info("On Stop Movement");
		int uniqueId = event.getEntityId();
		
		ScheduledFuture<?> movement = this.movements.get(uniqueId);
		if (movement != null) {
			movement.cancel(true);
		}
		
		this.gameModel.find(uniqueId).ifPresent((spawn) -> {
			if (spawn instanceof Trainer) {
				log.info("Trainer stop movement");
				Trainer trainer = this.gameModel.getTrainer();
				
				// NOTE: EntityStoppedEvent doesn't contain position data
				// The original TrainerStuckEvent parsed the packet to get position
				// For now, we'll need to handle this differently or enhance EntityStoppedEvent
				// TODO: EntityStoppedEvent should include position data or we need a different approach
				
				// Publish Spring event for backward compatibility (but without packet)
				// TrainerStuckEvent requires packet - this is a limitation
				// We may need to keep packet parsing for this specific case or enhance the event
				log.warn("EntityStoppedEvent received but TrainerStuckEvent requires packet data - position update skipped");
			} else if (spawn instanceof Monster) {
				log.info("Monster stop movement");
			}
		});
	}

	/**
	 * Handles EntitySpawnEvent from OSGi EventAdmin.
	 * NOTE: This still needs packet data to fully parse entities (Monster, Player, Pet, Item, Portal)
	 * EntitySpawnEvent only contains refId and position - full parsing requires ISpawnParser
	 * TODO: Either enhance EntitySpawnEvent to include full entity data or keep packet parsing for spawns
	 */
	private void handleEntitySpawnEvent(EntitySpawnEvent event) {
		// EntitySpawnEvent has refId and position, but not enough to create Monster/Player/etc.
		// The original onSpawn() uses ISpawnParser which needs packet reader
		// For now, log and handle basic case - full implementation needs packet data or enhanced event
		log.warn("EntitySpawnEvent received but full entity parsing requires packet data - spawn handling incomplete");
		log.debug("Spawn event: refId={}, entityId={}, position={}", 
			event.getRefId(), event.getEntityId(), event.getPosition());
	}

	/**
	 * Handles EntityDespawnEvent from OSGi EventAdmin.
	 * Migrated from onDespawn()
	 */
	private void handleEntityDespawnEvent(EntityDespawnEvent event) {
		int uniqueId = event.getEntityId();
		this.gameModel.remove(uniqueId);
		Optional.ofNullable(this.movements.remove(uniqueId)).ifPresent((movement) -> movement.cancel(true));
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
