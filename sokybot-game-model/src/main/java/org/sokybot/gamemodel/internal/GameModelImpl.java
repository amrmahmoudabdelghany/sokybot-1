package org.sokybot.gamemodel.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.commons.SilkroadUtils;
import org.sokybot.game.dto.MonsterData;
import org.sokybot.gameevents.events.entity.EntityAngleUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityMovementEvent;
import org.sokybot.gameevents.events.entity.EntitySpeedUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityStoppedEvent;
import org.sokybot.gameevents.events.spawn.MonsterSpawnEvent;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.machine.event.trainerevent.TrainerLoadedEvent;
import org.sokybot.persistence.entities.navmesh.Position;

public class GameModelImpl implements IGameModel, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(GameModelImpl.class);

    private final String machineName;
    private final BundleContext bundleContext;
    private ServiceRegistration<EventHandler> eventRegistration;
    
    // Internal mutable map
    private final Map<Integer, Spawn> spawns = new ConcurrentHashMap<>();
    private final Trainer trainer = new Trainer();
    
    // Movement handling
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2); // 2 threads enough?
    private final Map<Integer, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    
    private int selectedId = -1;

    public GameModelImpl(String machineName, BundleContext bundleContext) {
        this.machineName = machineName;
        this.bundleContext = bundleContext;
    }
    
    public void start() {
        Dictionary<String, Object> props = new Hashtable<>();
        String[] topics = new String[] {
             "sokybot/game/" + machineName + "/*",
             "sokybot/machine/spawn/monster",
             "sokybot/machine/despawn/monster",
             "sokybot/machine/trainer/loaded"
        };
        props.put(EventConstants.EVENT_TOPIC, topics);
        String filter = "(machineId=" + machineName + ")"; 
        // props.put(EventConstants.EVENT_FILTER, filter); // Uncomment if events have this property
        
        eventRegistration = bundleContext.registerService(EventHandler.class, this, props);
    }
    
    public void stop() {
        if (eventRegistration != null) {
            eventRegistration.unregister();
        }
        scheduler.shutdownNow();
    }

    @Override
    public Optional<ISpawn> find(int id) {
        if (id == trainer.getUniqueId()) return Optional.of(trainer);
        return Optional.ofNullable(spawns.get(id));
    }

    @Override
    public <T extends ISpawn> Map<Integer, T> findAll(Class<T> type) {
        Map<Integer, T> result = new HashMap<>();
        if (type.isInstance(trainer)) {
             result.put(trainer.getUniqueId(), type.cast(trainer));
        }
        spawns.values().stream()
            .filter(type::isInstance)
            .forEach(s -> result.put(s.getUniqueId(), type.cast(s)));
        return result;
    }

    @Override
    public <T extends ISpawn> Optional<T> find(int id, Class<T> type) {
         ISpawn s = null;
         if (id == trainer.getUniqueId()) s = trainer;
         else s = spawns.get(id);
         
         if (s != null && type.isInstance(s)) {
             return Optional.of(type.cast(s));
         }
         return Optional.empty();
    }

    @Override
    public Optional<ISpawn> getSelected() {
        return find(selectedId);
    }

    @Override
    public ITrainer getTrainer() {
        return trainer;
    }
    
    // ================= Event Handling =================

    @Override
    public void handleEvent(Event event) {
        try {
            Object eventObj = event.getProperty("event");
            if (eventObj == null) return;
            
            // Check Topic/Machine? Filter usually handles it but be safe
            // Assuming simplified check
            
            if (eventObj instanceof MonsterSpawnEvent) {
                handleMonsterSpawn((MonsterSpawnEvent) eventObj);
            } else if (eventObj instanceof EntityDespawnEvent) {
                handleDespawn((EntityDespawnEvent) eventObj);
            } else if (eventObj instanceof TrainerLoadedEvent) {
                // handleTrainerLoaded((TrainerLoadedEvent) eventObj); 
                // TrainerLoaded logic usually complex, involves setting ID etc.
                // For now, assume trainer is persistent and we just update it.
            } else if (eventObj instanceof EntityMovementEvent) {
                handleMovement((EntityMovementEvent) eventObj);
            } else if (eventObj instanceof EntityStoppedEvent) {
                handleStopped((EntityStoppedEvent) eventObj);
            } else if (eventObj instanceof EntityHPMPUpdateEvent) {
                handleHPMP((EntityHPMPUpdateEvent) eventObj);
            } else if (eventObj instanceof EntitySpeedUpdateEvent) {
                handleSpeed((EntitySpeedUpdateEvent) eventObj);
            } else if (eventObj instanceof EntityAngleUpdateEvent) {
                handleAngle((EntityAngleUpdateEvent) eventObj);
            }
        } catch (Exception e) {
            log.error("Error handling event in GameModelImpl", e);
        }
    }
    
    private void handleMonsterSpawn(MonsterSpawnEvent event) {
        MonsterData md = event.getMonster();
        Monster m = new Monster(md);
        // Initial pos calculation?
        // md likely has sectors/offsets.
        int x = SilkroadUtils.getXCoord(md.getXOffset(), md.getXSector());
        int y = SilkroadUtils.getYCoord(md.getYOffset(), md.getYSector());
        m.setDescription(md.getRefId() + ""); // hack?
        m.setLocation(x, y);
        m.setX(x); // Explicit internal setter
        m.setY(y);
        spawns.put(m.getUniqueId(), m);
    }
    
    private void handleDespawn(EntityDespawnEvent event) {
        int id = event.getEntityId();
        spawns.remove(id);
        stopMovement(id);
    }
    
    private void handleHPMP(EntityHPMPUpdateEvent event) {
        int id = event.getEntityId();
        if (id == trainer.getUniqueId()) {
            if (event.getNewHP() != null) trainer.setCharHP(event.getNewHP());
            if (event.getNewMP() != null) trainer.setCharMP(event.getNewMP());
        } else {
            Spawn s = spawns.get(id);
            if (s instanceof Fighter) {
                if (event.getNewHP() != null) ((Fighter)s).setCurrentHP(event.getNewHP());
                // MP?
            }
        }
    }
    
    private void handleSpeed(EntitySpeedUpdateEvent event) {
        int id = event.getEntityId();
        Fighter f = resolveFighter(id);
        if (f != null) {
            f.setWalkSpeed(event.getWalkSpeed());
            f.setRunSpeed(event.getRunSpeed());
            // Restart movement if moving?
        }
    }
    
    private void handleAngle(EntityAngleUpdateEvent event) {
        int id = event.getEntityId();
        Spawn s = resolveSpawn(id);
        if (s != null) {
            s.setAngle((short) event.getNewAngle());
        }
    }
    
    private Fighter resolveFighter(int id) {
        if (id == trainer.getUniqueId()) return trainer;
        Spawn s = spawns.get(id);
        if (s instanceof Fighter) return (Fighter) s;
        return null;
    }
    
    private Spawn resolveSpawn(int id) {
        if (id == trainer.getUniqueId()) return trainer;
        return spawns.get(id);
    }

    private void handleMovement(EntityMovementEvent event) {
        int id = event.getEntityId();
        Fighter fighter = resolveFighter(id);
        
        if (fighter != null) {
			boolean hasDestination = event.hasDestination();
			fighter.setHasDestination(hasDestination);
			
			if (event.getMovementType() != null) {
				fighter.setMovementType(event.getMovementType()); // Enum type check? Assuming shared package or same enum
                // MovementType is in game-enums, so it matches.
			}

			Position dest = event.getDestination();
			if (hasDestination && dest != null) {
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
				
				int destX = SilkroadUtils.getXCoord(destXOffset, destXSector);
				int destY = SilkroadUtils.getYCoord(destYOffset, destYSector);
				fighter.setDestX(destX);
				fighter.setDestY(destY);
			} else {
				if (event.getSkyClickFlag() != null) {
					fighter.setSkyClickFlag(event.getSkyClickFlag());
				}
                // Angle logic
                if (event.getAngleAction() != null && event.getSkyClickFlag() != null && event.getSkyClickFlag() == 1) {
					fighter.setAngle(event.getAngleAction());
				}
			}

			Position currentPos = event.getCurrentPosition();
			if (currentPos != null && event.getCurrentXSector() != null && event.getCurrentYSector() != null) {
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
				
				if (event.getCurrentAngle() != null) {
                    // Need Util match
					fighter.setAngle(org.sokybot.commons.SilkroadUtils.getAngle(event.getCurrentAngle())); 
				}
				
				int x = SilkroadUtils.getXCoord(xOffset, xSector); // overload with 100?
				int y = SilkroadUtils.getYCoord(yOffset, ySector);
				fighter.setLocation(x, y);
			}

			startMovement(fighter);
        }
    }
    
    private void handleStopped(EntityStoppedEvent event) {
        int id = event.getEntityId();
        stopMovement(id);
        // Optional: snap to final pos?
    }
    
    private void stopMovement(int id) {
        ScheduledFuture<?> task = tasks.remove(id);
        if (task != null) {
            task.cancel(true);
        }
    }
    
    private void startMovement(Fighter fighter) {
        stopMovement(fighter.getUniqueId());
        
        // Only animate if destination exists or forced
        // Logic from EnvironmentHandler
        
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
			double angle = 0; 
			if (fighter.isHasDestination()) {
				angle = Math.atan2(fighter.getDestY() - fighter.getY(), fighter.getDestX() - fighter.getX());
			} else {
				angle = Math.toRadians(fighter.getAngle());
			}

			if (angle != 0) {
                // Speed factor? 0.1?
                // Logic: 1000 / (runSpeed * 0.1) ms delay? 
                // Wait, scheduleAtFixedRate is (runnable, init, period).
                // If period is calculated inside? No, period is fixed.
                // EnvironmentHandler calculated period: (long) (1000 / (fighter.getRunSpeed() * 0.1))
                // This assumes constant speed.
                
                // We perform ONE step here.
				fighter.translate((int) Math.round(Math.cos(angle)), (int) Math.round(Math.sin(angle)));
                // Update specific internal logic if needed
                int newX = fighter.getX() + (int) Math.round(Math.cos(angle));
                int newY = fighter.getY() + (int) Math.round(Math.sin(angle));
                fighter.setLocation(newX, newY);
                
			} else {
                stopMovement(fighter.getUniqueId());
			}
        }, 0, calculatePeriod(fighter), TimeUnit.MILLISECONDS);
        
        tasks.put(fighter.getUniqueId(), task);
    }
    
    private long calculatePeriod(Fighter f) {
        float speed = f.getRunSpeed(); 
        // fallback
        if (speed <= 0) speed = 50f; 
        return (long) (1000 / (speed * 0.1));
    }

}
