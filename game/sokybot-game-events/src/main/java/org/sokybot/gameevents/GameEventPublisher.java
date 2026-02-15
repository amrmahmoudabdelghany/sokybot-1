package org.sokybot.gameevents;

import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.IPacketObserver;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.proxy.IProxyConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * OSGi Declarative Services component that translates packets to events.
 * Dynamically discovers and registers all IPacketTranslator implementations.
 * Subscribes to IProxyConnection packet publishers.
 */
@Component(immediate = true, service = { GameEventPublisher.class })
public class GameEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(GameEventPublisher.class);

    private final Map<Integer, IPacketTranslator> translators = new ConcurrentHashMap<>();
    private final Map<String, ConnectionEntry> connections = new ConcurrentHashMap<>();

    private EventAdmin eventAdmin;

    @Reference
    protected void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    /**
     * Dynamically bind all translator implementations as they become available.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindTranslator(IPacketTranslator translator) {
        int opcode = translator.getOpcode();
        translators.put(opcode, translator);
        log.info("Registered packet translator for opcode: 0x{}",
                Integer.toHexString(opcode).toUpperCase());

        // Update all existing connections to subscribe to this new opcode
        for (ConnectionEntry entry : connections.values()) {
            subscribeToOpcode(entry, opcode);
        }
    }

    protected void unbindTranslator(IPacketTranslator translator) {
        translators.remove(translator.getOpcode());
        // We generally don't unsubscribe individually for simplicity,
        // as the translator map lookup will just return null and we ignore it.
        log.info("Unregistered packet translator for opcode: 0x{}",
                Integer.toHexString(translator.getOpcode()).toUpperCase());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindProxyConnection(IProxyConnection connection) {
        String machineId = connection.getMachineId();

        ConnectionEntry entry = new ConnectionEntry(connection);
        connections.put(machineId, entry);

        // Subscribe to all current translators
        int[] opcodes = translators.keySet().stream().mapToInt(i -> i).toArray();
        if (opcodes.length > 0) {
            IPacketSubscription sub = connection.getPacketPublisher().subscribe(entry.observer, opcodes);
            entry.subscriptions.add(sub);
        }

        log.info("Bound proxy connection for machine: {}", machineId);
    }

    protected void unbindProxyConnection(IProxyConnection connection) {
        // Find entry by connection instance?
        // Or if IProxyConnection has no ID in interface, how do I find it?
        // I need to iterate or add getMachineId.
        // I WILL FIX INTERFACE IN NEXT STEP.

        connections.values().removeIf(entry -> {
            boolean match = entry.connection == connection;
            if (match) {
                entry.unsubscribeAll();
            }
            return match;
        });
        log.info("Unbound proxy connection");
    }

    private void subscribeToOpcode(ConnectionEntry entry, int opcode) {
        IPacketSubscription sub = entry.connection.getPacketPublisher().subscribe(opcode, entry.observer);
        entry.subscriptions.add(sub);
    }

    @Activate
    protected void activate() {
        log.info("GameEventPublisher activated");
    }

    @Deactivate
    protected void deactivate() {
        connections.values().forEach(ConnectionEntry::unsubscribeAll);
        connections.clear();
        log.info("GameEventPublisher deactivated");
    }

    private void onPacketReceived(String machineFullName, ImmutablePacket packet) {
        int opcode = packet.getOpcode();
        IPacketTranslator translator = translators.get(opcode);

        if (translator != null) {
            try {
                // Get chunk manager for this machine (may be null if not registered yet)
                ChunkedPacketManager chunkManager = ChunkedPacketManagerRegistry.getInstance().get(machineFullName);

                java.util.List<IGameEvent> events = translator.translate(machineFullName, packet, chunkManager);
                for (IGameEvent gameEvent : events) {
                    if (gameEvent != null) {
                        publishEvent(machineFullName, gameEvent);
                    }
                }
            } catch (Exception e) {
                log.error("Error translating packet 0x{} from {}: {}",
                        Integer.toHexString(opcode).toUpperCase(),
                        machineFullName,
                        e.getMessage(), e);
            }
        }
    }

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

    // Internal Helper
    private class ConnectionEntry {
        final IProxyConnection connection;
        final IPacketObserver observer;
        final List<IPacketSubscription> subscriptions = new CopyOnWriteArrayList<>();

        ConnectionEntry(IProxyConnection connection) {
            this.connection = connection;
            String id = connection.getMachineId();
            this.observer = packet -> onPacketReceived(id, packet);
        }

        void unsubscribeAll() {
            subscriptions.forEach(IPacketSubscription::unsubscribe);
            subscriptions.clear();
        }
    }
}
