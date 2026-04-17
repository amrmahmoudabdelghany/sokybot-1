package org.sokybot.http.server.events;

import java.time.Instant;
import java.util.Map;

import org.sokybot.commons.topic.Topic;
import org.sokybot.commons.topic.TopicMatcher;

/**
 * Event payload for the event bridge.
 */
public class BridgeEvent<T> {
    private final Topic topicObj;
    private final String machineId;
    private final T payload;
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    public BridgeEvent(Topic topic, T payload) {
        this(topic, null, payload, null);
    }

    public BridgeEvent(Topic topic, String machineId, T payload) {
        this(topic, machineId, payload, null);
    }

    public BridgeEvent(Topic topic, String machineId, T payload, Map<String, Object> metadata) {
        this.topicObj = topic == null ? Topic.parse("**") : topic;
        this.machineId = machineId;
        this.payload = payload;
        this.timestamp = Instant.now();
        this.metadata = metadata;
    }

    @Deprecated
    public BridgeEvent(String topic, T payload) {
        this(Topic.parse(topic), null, payload, null);
    }

    @Deprecated
    public BridgeEvent(String topic, String machineId, T payload) {
        this(Topic.parse(topic), machineId, payload, null);
    }

    @Deprecated
    public BridgeEvent(String topic, String machineId, T payload, Map<String, Object> metadata) {
        this(Topic.parse(topic), machineId, payload, metadata);
    }

    public Topic getTopicObj() {
        return topicObj;
    }

    public String getTopic() {
        return topicObj.toBridgeString();
    }

    public String getMachineId() {
        return machineId;
    }

    public T getPayload() {
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
    public boolean matchesTopic(Topic pattern) {
        return TopicMatcher.DEFAULT.matches(pattern, this.topicObj);
    }

    @Deprecated
    public boolean matchesTopic(String pattern) {
        return TopicMatcher.DEFAULT.matches(pattern, getTopic());
    }

    @Override
    public String toString() {
        return String.format("BridgeEvent{topic='%s', machineId='%s', timestamp=%s}", getTopic(), machineId, timestamp);
    }
}
