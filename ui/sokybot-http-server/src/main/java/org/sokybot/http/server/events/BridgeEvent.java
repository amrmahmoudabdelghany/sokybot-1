package org.sokybot.http.server.events;

import java.time.Instant;
import java.util.Map;

/**
 * Event payload for the event bridge.
 */
public class BridgeEvent {

    private final String topic;
    private final String machineId;
    private final Object payload;
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    public BridgeEvent(String topic, Object payload) {
        this(topic, null, payload, null);
    }

    public BridgeEvent(String topic, String machineId, Object payload) {
        this(topic, machineId, payload, null);
    }

    public BridgeEvent(String topic, String machineId, Object payload, Map<String, Object> metadata) {
        this.topic = topic;
        this.machineId = machineId;
        this.payload = payload;
        this.timestamp = Instant.now();
        this.metadata = metadata;
    }

    public String getTopic() {
        return topic;
    }

    public String getMachineId() {
        return machineId;
    }

    public Object getPayload() {
        return payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Check if this event matches a topic pattern.
     * Patterns support wildcards: "*" matches single level, "**" matches multiple
     * levels.
     */
    public boolean matchesTopic(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }
        if (pattern.equals("**")) {
            return true;
        }
        if (pattern.equals(topic)) {
            return true;
        }
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return topic.startsWith(prefix + ".") && !topic.substring(prefix.length() + 1).contains(".");
        }
        if (pattern.endsWith(".**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return topic.startsWith(prefix + ".") || topic.equals(prefix);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("BridgeEvent{topic='%s', machineId='%s', timestamp=%s}",
                topic, machineId, timestamp);
    }
}
