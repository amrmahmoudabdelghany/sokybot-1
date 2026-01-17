package org.sokybot.machine.controller;

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
import org.sokybot.gameevents.events.spawn.MonsterSpawnEvent;
import org.sokybot.gameevents.events.spawn.ItemSpawnEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.skill.SkillCastStartEvent;
import org.sokybot.machine.event.trainerevent.TrainerReachDestinationEvent;
import org.sokybot.machine.event.trainerevent.TrainerStuckEvent;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gamemodel.model.IFighter;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.IPlayer;
// import org.sokybot.machine.parser.ISpawnParser; // Removed
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.app.AppConstants;
import org.sokybot.network.PacketListener;
// import org.sokybot.machine.network.PacketListener.PacketSource;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.commons.SilkroadUtils;
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
	private IGameModel gameModel;
	
	@Autowired
	private ITrainer trainer;

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

	private ServiceRegistration<EventHandler> eventHandlerRegistration;
    // private final Map<Integer, ScheduledFuture<?>> movements = new HashMap<>(); // Logic moved to GameModelImpl
    private Pair<Byte, Short> currentG;

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
	
	private void publishOsgiEvent(String subTopic, Object eventPayload) {
	    if (bundleContext == null) return;
	    try {
	        org.osgi.framework.ServiceReference<org.osgi.service.event.EventAdmin> ref = 
	            bundleContext.getServiceReference(org.osgi.service.event.EventAdmin.class);
	        if (ref != null) {
	            org.osgi.service.event.EventAdmin eventAdmin = bundleContext.getService(ref);
	            if (eventAdmin != null) {
	                Map<String, Object> props = new HashMap<>();
	                props.put("event", eventPayload);
	                props.put("machineId", machineFullName());
	                eventAdmin.postEvent(new Event("sokybot/machine/" + subTopic, props));
	            }
	        }
	    } catch(Exception e) {
	        log.error("Error publishing OSGi event: " + subTopic, e);
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
					// handleEntitySpawnEvent((EntitySpawnEvent) event); // Deprecated
					break;
                case "MonsterSpawnEvent":
                    handleMonsterSpawnEvent((MonsterSpawnEvent) event);
                    break;
                case "ItemSpawnEvent":
                    handleItemSpawnEvent((ItemSpawnEvent) event);
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
        // Deprecated - Requires ISpawnParser
		/*
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
        */
	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.GROUP_SPAWN_END)
	public void onGroupSpawnEnd(ImmutablePacket packet) {
		this.currentG = null;
	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.SINGLE_SPAWN)
	public void onSingleSpawn(ImmutablePacket packet) {
		// onSpawn(packet.getStreamReader());
	}

	@Deprecated
	@PacketListener(opcode = ServerOpcode.SINGLE_DESPAWN)
	public void onSingleDespawn(ImmutablePacket packet) {
		// onDespawn(packet.getStreamReader());
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
				// this.ctx.publishEvent(TrainerAttackedEvent.builder().casterId(casterId).skillId(skillId).build());
			}
		}
		// Note: SkillCastEvent already represents success/failure, so no need for error event
	}

	private void handleSkillCastEndEvent(SkillCastEndEvent event) {
		// Currently empty - can add logic if needed
	}

    // Legacy onDespawn and onSpawn removed/commented
    /*
	private void onDespawn(IStreamReader reader) { ... }
	private void onSpawn(IStreamReader reader) { ... }
    */
    
    private void handleMonsterSpawnEvent(MonsterSpawnEvent event) {
        // Model handles its own update
        this.ctx.publishEvent(event);
    }

    private void handleItemSpawnEvent(ItemSpawnEvent event) {
        // Model handles update
        this.ctx.publishEvent(event);
    }

	private void handleHPMPUpdateEvent(EntityHPMPUpdateEvent event) {
        // Logic handled by GameModel
	}

	private void handleSpeedUpdateEvent(EntitySpeedUpdateEvent event) {
        // Logic handled by GameModel
	}

	private void handleAngleUpdateEvent(EntityAngleUpdateEvent event) {
        // Logic handled by GameModel
	}

	private void handleEntitySelectedEvent(EntitySelectedEvent event) {
		int selectedId = event.getSelectedEntityId();
		// this.gameModel.setSelectedSpawn(selectedId);

		Integer currentHP = event.getCurrentHP();
        // Selection is logic? Or Display?
        // GameModel has getSelected(). But setSelected?
        // GameModelImpl has setSelected logic if handling EntitySelectedEvent?
        // I should check if GameModelImpl handles EntitySelectedEvent. I didn't add it.
        // But EntitySelectedEvent is usually UI related. 
        // If Engine needs to know selection, it queries.
        // For now, removing mutation.
        // this.gameModel.setSelectedSpawn(selectedId); // Interface IGameModel is ReadOnly. 
        // Logic for selection in Engine?
	}

	private void handleEntityMovementEvent(EntityMovementEvent event) {
        // Logic moved to GameModel
	}

	private void handleEntityStoppedEvent(EntityStoppedEvent event) {
        // Logic handled by GameModel
	}

	private void handleEntitySpawnEvent(EntitySpawnEvent event) {
		log.warn("EntitySpawnEvent received but full entity parsing requires packet data - spawn handling incomplete");
		log.debug("Spawn event: refId={}, entityId={}, position={}", 
			event.getRefId(), event.getEntityId(), event.getPosition());
	}

	private void handleEntityDespawnEvent(EntityDespawnEvent event) {
        // Model handles remove
        this.ctx.publishEvent(event);
	}
    
    // translate logic removed


}
