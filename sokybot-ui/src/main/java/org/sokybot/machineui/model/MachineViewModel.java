package org.sokybot.machineui.model;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent;
import org.sokybot.gameevents.events.entity.EntityMovementEvent;
import org.sokybot.machine.event.monsterevent.MonsterDespawnEvent;
import org.sokybot.machine.event.monsterevent.MonsterSpawnEvent;
import org.sokybot.machine.event.trainerevent.TrainerLoadedEvent;
import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.utils.SilkroadUtils;

public class MachineViewModel implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(MachineViewModel.class);
    
    // Observable pattern or simple getters?
    // For Swing, maybe PropertyChangeSupport?
    // For now, let's just keep state for polling/painting.
    
    private Trainer trainer;
    private final Map<Integer, Monster> monsters = new ConcurrentHashMap<>();
    
    private final String machineFullName;
    private final BundleContext bundleContext;
    private ServiceRegistration<EventHandler> eventRegistration;
    
    private final Vector<Runnable> monsterListeners = new Vector<>();
    private final Vector<Runnable> trainerListeners = new Vector<>();

    public MachineViewModel(String machineFullName, BundleContext bundleContext) {
        this.machineFullName = machineFullName;
        this.bundleContext = bundleContext;
        // Initialize default trainer
        this.trainer = new Trainer(); 
        init();
    }
    
    private void init() {
         Dictionary<String, Object> props = new Hashtable<>();
         props.put(EventConstants.EVENT_TOPIC, "sokybot/game/" + machineFullName + "/*");
         // Also listen to specific spawn topics if published differently
         // "sokybot/machine/spawn/monster" was used in EnvironmentHandler
         // Pattern was: "sokybot/machine/" + subTopic.
         // Wait, EnvironmentHandler publishes to "sokybot/machine/..." with props "machineId=..."
         // The topic pattern in EnvironmentHandler init was "sokybot/game/" + machineFullName + "/*"
         // But my publishOsgiEvent used "sokybot/machine/" + subTopic
         // This is inconsistent. I should fix EnvironmentHandler to use consistent topic.
         // Or listen to BOTH.
         // Let's listen to both for now.
         
         String[] topics = new String[] {
             "sokybot/game/" + machineFullName + "/*",
             "sokybot/machine/spawn/monster",
             "sokybot/machine/despawn/monster",
             "sokybot/machine/trainer/loaded" 
         };
         props.put(EventConstants.EVENT_TOPIC, topics);
         
         // Filter by machineId for "sokybot/machine/*" topics
         String filter = "(machineId=" + machineFullName + ")";
         // props.put(EventConstants.EVENT_FILTER, filter); // Filter applies to all topics?
         // Filter applies if properties match.
         
         eventRegistration = bundleContext.registerService(EventHandler.class, this, props);
    }
    
    public void dispose() {
        if (eventRegistration != null) eventRegistration.unregister();
    }
    
    public Trainer getTrainer() {
        return trainer;
    }
    
    public Map<Integer, Monster> getMonsters() {
        return monsters; 
    }
    
    public void addMonsterListener(Runnable r) {
        monsterListeners.add(r);
    }
    
    public void addTrainerListener(Runnable r) {
        trainerListeners.add(r);
    }

    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        Object eventObj = event.getProperty("event");
        String eventMachineId = (String) event.getProperty("machineId");
        
        // Check machine ID if present (for generic topics)
        if (eventMachineId != null && !eventMachineId.equals(machineFullName)) {
            return;
        }
        
        // Handle publishing logic from EnvironmentHandler
        if (eventObj instanceof MonsterSpawnEvent) {
            Monster m = ((MonsterSpawnEvent) eventObj).getMonster();
            monsters.put(m.getUniqueId(), m);
            notifyMonsterListeners();
        } else if (eventObj instanceof MonsterDespawnEvent) {
            Monster m = ((MonsterDespawnEvent) eventObj).getMonster();
            monsters.remove(m.getUniqueId());
            notifyMonsterListeners();
        } else if (eventObj instanceof TrainerLoadedEvent) {
             this.trainer = ((TrainerLoadedEvent) eventObj).getTraienr();
             notifyTrainerListeners();
        }
        
        // Handle game-events (EntityMovement, HPMP, etc)
        // These wraps raw packets usually? No, game-events are POJOs.
         if (eventObj instanceof EntityMovementEvent) {
             EntityMovementEvent move = (EntityMovementEvent) eventObj;
             // Update trainer pos if it matches
             // We don't know uniqueId of trainer unless we have it.
             // Trainer object has uniqueId.
             if (trainer != null && move.getEntityId() == trainer.getUniqueId()) {
                 updateTrainerPos(move);
                 notifyTrainerListeners();
             }
             // For monsters, we might assume they are updated by reference? 
             // IF Monster objects in 'monsters' map are the SAME instances as in the event (passed by ref in OSGi same JVM),
             // then updates might happen automatically if the publisher updates the object.
             // EnvironmentHandler updates the object in GameModel.
             // Does it publish the SAME object? Yes.
             // So if we hold reference, we see updates.
             // Except Position?
             // EntityMovementEvent has pos data. EnvironmentHandler updates the fighter object.
             // So yes, we should rely on object reference updates mostly, 
             // but 'repaint' triggers are needed.
         }
         
         // Repaint on any event?
         // Maybe too frequent.
         
    }
    
    private void updateTrainerPos(EntityMovementEvent event) {
        // Logic similar to EnvironmentHandler to calc world pos
        Position currentPos = event.getCurrentPosition();
        if (currentPos != null && event.getCurrentXSector() != null) {
             // trainer.set...
             // Simplified: rely on shared object state if possible, or update simply.
             // Actually, EnvironmentHandler logic is needed to convert offset+sector to world X/Y.
             // SilkroadUtils.getXCoord...
             int x = SilkroadUtils.getXCoord(currentPos.getX(), event.getCurrentXSector().byteValue());
             int y = SilkroadUtils.getYCoord(currentPos.getY(), event.getCurrentYSector().byteValue());
             trainer.setX(x);
             trainer.setY(y);
        }
    }

    private void notifyMonsterListeners() {
        monsterListeners.forEach(Runnable::run);
    }
    
    private void notifyTrainerListeners() {
        trainerListeners.forEach(Runnable::run);
    }
}
