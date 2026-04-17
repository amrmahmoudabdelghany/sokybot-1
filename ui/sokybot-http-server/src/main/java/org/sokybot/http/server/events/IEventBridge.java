package org.sokybot.http.server.events;

import java.util.function.Consumer;

import org.sokybot.commons.topic.Topic;

/**
 * Interface for publishing events to connected clients.
 * Implements a publish-subscribe pattern for real-time updates.
 */
public interface IEventBridge {
    <T> void publish(Topic topic, T event);

    <T> void publish(String machineId, Topic topic, T event);

    <T> Subscription subscribe(Topic pattern, Consumer<BridgeEvent<T>> callback);

    <T> Subscription subscribe(Topic pattern, Class<T> payloadType, Consumer<BridgeEvent<T>> callback);

    <T> Subscription subscribe(String machineId, Topic pattern, Consumer<BridgeEvent<T>> callback);

    <T> Subscription subscribe(String machineId, Topic pattern, Class<T> payloadType, Consumer<BridgeEvent<T>> callback);

    /**
     * Publish an event to all subscribers matching the topic.
     * 
     * @param topic the event topic (e.g., "machine.started", "character.levelUp")
     * @param event the event payload
     */
    @Deprecated
    default void publish(String topic, Object event) {
        publish(Topic.parse(topic), event);
    }

    /**
     * Publish an event scoped to a specific machine.
     * 
     * @param machineId the machine ID
     * @param topic     the event topic
     * @param event     the event payload
     */
    @Deprecated
    default void publish(String machineId, String topic, Object event) {
        publish(machineId, Topic.parse(topic), event);
    }

    /**
     * Subscribe to events matching a topic pattern.
     * Pattern supports wildcards: "*" matches single level, "**" matches multiple
     * levels.
     * 
     * @param pattern  the topic pattern (e.g., "machine.*", "character.**")
     * @param callback the callback to invoke when matching events are published
     * @return a subscription that can be cancelled
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    default Subscription subscribe(String pattern, Consumer<BridgeEvent> callback) {
        return subscribe(Topic.parse(pattern), (Consumer<BridgeEvent<Object>>) (Consumer<?>) callback);
    }

    /**
     * Subscribe to events for a specific machine.
     * 
     * @param machineId the machine ID to filter
     * @param pattern   the topic pattern
     * @param callback  the callback
     * @return a subscription
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    default Subscription subscribe(String machineId, String pattern, Consumer<BridgeEvent> callback) {
        return subscribe(machineId, Topic.parse(pattern), (Consumer<BridgeEvent<Object>>) (Consumer<?>) callback);
    }

    /**
     * Get the number of active subscribers.
     */
    int getSubscriberCount();

    /**
     * Get the total number of events published since startup.
     */
    long getEventCount();

    /**
     * Subscription handle for cancellation.
     */
    interface Subscription extends org.sokybot.commons.lifecycle.Subscription {
        /**
         * Unsubscribe from events.
         */
        void unsubscribe();

        /**
         * Check if still active.
         */
        @Override
        boolean isActive();

        @Override
        default void close() {
            unsubscribe();
        }
    }
}
