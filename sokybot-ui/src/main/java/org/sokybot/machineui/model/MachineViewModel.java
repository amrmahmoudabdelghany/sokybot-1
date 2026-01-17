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
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.sokybot.gameevents.events.spawn.MonsterSpawnEvent;
import org.sokybot.machine.event.trainerevent.TrainerLoadedEvent;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.commons.SilkroadUtils;

public class MachineViewModel implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(MachineViewModel.class);
    
    private IGameModel gameModel;
    // private final Map<Integer, Monster> monsters = new ConcurrentHashMap<>(); // Removed: Use IGameModel
    private final String machineFullName;
    private final BundleContext bundleContext;
    private ServiceRegistration<EventHandler> eventRegistration;
    
    private final Vector<Runnable> monsterListeners = new Vector<>();
    private final Vector<Runnable> trainerListeners = new Vector<>();

        this.machineFullName = machineFullName;
        this.bundleContext = bundleContext;
        // Logic will inject model or lookup model service from machine context
        // Assuming IGameModel is available as OSGi service for this machine
        // Using declarative services or manual lookup in init
        init();
    }
    
    private void init() {
         // Register to listen to UI-relevant events
         Dictionary<String, Object> props = new Hashtable<>();
         String[] topics = new String[] {
             "sokybot/game/" + machineFullName + "/*",
             "sokybot/machine/spawn/monster",
             "sokybot/machine/despawn/monster",
             "sokybot/machine/trainer/loaded" 
         };
         props.put(EventConstants.EVENT_TOPIC, topics);
         
         eventRegistration = bundleContext.registerService(EventHandler.class, this, props);
         
         // Lookup GameModel service?
         // Or pass it in constructor? 
         // Since this is UI, created by UIActivator maybe?
         // For now, let's assume we can lookup IGameModelFactory or IGameModel directly
         // But IGameModel is one per machine. 
         // We need to filter by machineName?  
         // Actually, if we use OSGi, better to Reference it.
         // But MachineViewModel seems to be manually instantiated.
         // We will lookup the service using machineName filter.
         findGameModel();
    }
    
    public void dispose() {
        if (eventRegistration != null) eventRegistration.unregister();
    }
    
    public ITrainer getTrainer() {
        return gameModel != null ? gameModel.getTrainer() : null;
    }
    
    public Map<Integer, IMonster> getMonsters() {
        return gameModel != null ? gameModel.findAll(IMonster.class) : Collections.emptyMap(); 
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
        
        if (eventObj instanceof MonsterSpawnEvent) {
            notifyMonsterListeners();
        } else if (eventObj instanceof EntityDespawnEvent) {
            notifyMonsterListeners();
        } else if (eventObj instanceof TrainerLoadedEvent) {
             notifyTrainerListeners();
        } else if (eventObj instanceof EntityMovementEvent) {
             EntityMovementEvent move = (EntityMovementEvent) eventObj;
             ITrainer trainer = getTrainer();
             if (trainer != null && move.getEntityId() == trainer.getUniqueId()) {
                 notifyTrainerListeners();
             }
         }
    }
    
    private void findGameModel() {
         // Manual lookup hack for now, assuming 1:1 match or filter later
         // Or use ServiceTracker
         try {
             org.osgi.framework.ServiceReference<IGameModel>[] refs = (org.osgi.framework.ServiceReference<IGameModel>[])
                 bundleContext.getServiceReferences(IGameModel.class.getName(), null);
             if (refs != null) {
                 for(org.osgi.framework.ServiceReference<IGameModel> ref : refs) {
                     // Check machine property if set in GameModelImpl
                     // For now just grab first
                     this.gameModel = bundleContext.getService(ref);
                 }
             }
         } catch(Exception e) {
             log.error("Failed to lookup GameModel", e);
         }
    }

    private void notifyMonsterListeners() {
        monsterListeners.forEach(Runnable::run);
    }
    
    private void notifyTrainerListeners() {
        trainerListeners.forEach(Runnable::run);
    }
}
