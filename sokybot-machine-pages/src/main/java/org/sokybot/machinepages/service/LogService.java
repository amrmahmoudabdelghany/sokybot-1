package org.sokybot.machinepages.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service for managing log page state and handling all game events for logging.
 */
public class LogService implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);
    
    private final String machineFullName;
    private final Sinks.Many<Map<String, Object>> eventSink = 
        Sinks.many().multicast().onBackpressureBuffer(1000);
    private final List<Map<String, Object>> eventBuffer = new ArrayList<>();
    private static final int MAX_EVENTS = 100;
    private int maxEvents = MAX_EVENTS;
    
    public LogService(String machineFullName) {
        this.machineFullName = machineFullName;
    }
    
    @Override
    public void handleEvent(Event osgiEvent) {
        String fullName = (String) osgiEvent.getProperty("fullName");
        if (fullName == null || !machineFullName.equals(fullName)) {
            return; // Not for this machine
        }
        
        // Convert OSGi event to log entry
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("eventType", osgiEvent.getProperty("eventType"));
        logEntry.put("timestamp", osgiEvent.getProperty("timestamp"));
        logEntry.put("fullName", fullName);
        logEntry.put("groupName", osgiEvent.getProperty("groupName"));
        logEntry.put("machineName", osgiEvent.getProperty("machineName"));
        
        // Try to serialize the event object
        Object eventObj = osgiEvent.getProperty("event");
        if (eventObj != null) {
            try {
                // Convert event to string representation
                logEntry.put("event", eventObj.toString());
            } catch (Exception e) {
                logEntry.put("event", "Error serializing event: " + e.getMessage());
            }
        }
        
        // Add to buffer (keep last N events)
        synchronized (eventBuffer) {
            eventBuffer.add(0, logEntry); // Add to front
            while (eventBuffer.size() > maxEvents) {
                eventBuffer.remove(eventBuffer.size() - 1); // Remove from back
            }
        }
        
        // Emit to stream
        Map<String, Object> streamData = new HashMap<>();
        streamData.put("events", new ArrayList<>(eventBuffer));
        eventSink.tryEmitNext(streamData);
    }
    
    public Map<String, Object> handleAction(String action, Map<String, Object> data) {
        Map<String, Object> newState = new HashMap<>();
        switch (action) {
            case "clearLog":
                synchronized (eventBuffer) {
                    eventBuffer.clear();
                }
                newState.put("events", new ArrayList<>());
                emitStateUpdate();
                return Map.of("success", true, "state", newState);
            case "setLogLevel":
                // TODO: Implement log level filtering
                return Map.of("success", false, "error", "Not implemented");
            case "setMaxEvents":
                int newMax = ((Number) data.getOrDefault("value", MAX_EVENTS)).intValue();
                this.maxEvents = Math.max(1, Math.min(1000, newMax)); // Clamp between 1 and 1000
                synchronized (eventBuffer) {
                    while (eventBuffer.size() > maxEvents) {
                        eventBuffer.remove(eventBuffer.size() - 1);
                    }
                }
                newState.put("maxEvents", maxEvents);
                newState.put("events", new ArrayList<>(eventBuffer));
                emitStateUpdate();
                return Map.of("success", true, "state", newState);
            default:
                return Map.of("success", false, "error", "Unknown action: " + action);
        }
    }
    
    public Flux<Map<String, Object>> streamLog() {
        // Return initial state + updates
        return Flux.concat(
            Flux.just(getLogData()),
            eventSink.asFlux()
        );
    }
    
    private Map<String, Object> getLogData() {
        Map<String, Object> data = new HashMap<>();
        synchronized (eventBuffer) {
            data.put("events", new ArrayList<>(eventBuffer));
        }
        data.put("maxEvents", maxEvents);
        return data;
    }
    
    private void emitStateUpdate() {
        eventSink.tryEmitNext(getLogData());
    }
    
    public Map<String, Object> getInitialState() {
        return getLogData();
    }
    
    public void shutdown() {
        eventSink.tryEmitComplete();
    }
}
