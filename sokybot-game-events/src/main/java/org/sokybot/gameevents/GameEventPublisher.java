package org.sokybot.gameevents;

import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSGi Declarative Services component that translates packets to events.
 * Dynamically discovers and registers all IPacketTranslator implementations.
 */
@Component(
    immediate = true,
    service = GameEventPublisher.class
)
public class GameEventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(GameEventPublisher.class);
    
    private final Map<Integer, IPacketTranslator> translators = new ConcurrentHashMap<>();
    
    private EventAdmin eventAdmin;
    
    @Reference
    protected void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }
    
    /**
     * Dynamically bind all translator implementations as they become available.
     */
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void bindTranslator(IPacketTranslator translator) {
        translators.put(translator.getOpcode(), translator);
        log.info("Registered packet translator for opcode: 0x{}", 
            Integer.toHexString(translator.getOpcode()).toUpperCase());
    }
    
    protected void unbindTranslator(IPacketTranslator translator) {
        translators.remove(translator.getOpcode());
        log.info("Unregistered packet translator for opcode: 0x{}", 
            Integer.toHexString(translator.getOpcode()).toUpperCase());
    }
    
    @Activate
    protected void activate() {
        log.info("GameEventPublisher activated with {} translators", translators.size());
    }
    
    @Deactivate
    protected void deactivate() {
        log.info("GameEventPublisher deactivated");
    }
    
    /**
     * Called by machine contexts when they receive a packet.
     * Translates the packet to domain events and publishes them via OSGi EventAdmin.
     * 
     * @param machineFullName The full name of the machine (groupName.machineName)
     * @param packet The raw immutable packet received from proxy
     */
    public void onPacketReceived(String machineFullName, ImmutablePacket packet) {
        int opcode = packet.getOpcode();
        IPacketTranslator translator = translators.get(opcode);
        
        if (translator != null) {
            try {
                java.util.List<IGameEvent> events = translator.translate(machineFullName, packet);
                for (IGameEvent event : events) {
                    if (event != null) {
                        publishEvent(machineFullName, event);
                    }
                }
            } catch (Exception e) {
                log.error("Error translating packet 0x{} from {}: {}", 
                    Integer.toHexString(opcode).toUpperCase(), 
                    machineFullName, 
                    e.getMessage(), e);
            }
        }
        // No translator registered for this opcode - normal for un-mapped packets
    }
    
    /**
     * Publishes a game event to OSGi EventAdmin with proper topic and properties.
     */
    private void publishEvent(String machineFullName, IGameEvent event) {
        String eventType = event.getClass().getSimpleName();
        String topic = "sokybot/game/" + machineFullName + "/" + eventType;
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("event", event);
        properties.put("fullName", machineFullName);
        properties.put("groupName", event.getGroupName());
        properties.put("machineName", event.getMachineName());
        properties.put("timestamp", event.getTimestamp());
        properties.put("eventType", eventType);
        
        Event osgiEvent = new Event(topic, properties);
        eventAdmin.postEvent(osgiEvent);
        
        log.debug("Published event: {} from {}", eventType, machineFullName);
    }
}
