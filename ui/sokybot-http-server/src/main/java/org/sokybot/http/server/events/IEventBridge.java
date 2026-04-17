package org.sokybot.http.server.events;

import java.util.function.Consumer;

/**
 * Interface for publishing events to connected clients.
 * Implements a publish-subscribe pattern for real-time updates.
 */
public interface IEventBridge {

    /**
     * Publish an event to all subscribers matching the topic.
     * 
     * @param topic the event topic (e.g., "machine.started", "character.levelUp")
     * @param event the event payload
     */
    void publish(String topic, Object event);

    /**
     * Publish an event scoped to a specific machine.
     * 
     * @param machineId the machine ID
     * @param topic     the event topic
     * @param event     the event payload
     */
    void publish(String machineId, String topic, Object event);

    /**
     * Subscribe to events matching a topic pattern.
     * Pattern supports wildcards: "*" matches single level, "**" matches multiple
     * levels.
     * 
     * @param pattern  the topic pattern (e.g., "machine.*", "character.**")
     * @param callback the callback to invoke when matching events are published
     * @return a subscription that can be cancelled
     */
    Subscription subscribe(String pattern, Consumer<BridgeEvent> callback);

    /**
     * Subscribe to events for a specific machine.
     * 
     * @param machineId the machine ID to filter
     * @param pattern   the topic pattern
     * @param callback  the callback
     * @return a subscription
     */
    Subscription subscribe(String machineId, String pattern, Consumer<BridgeEvent> callback);

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
