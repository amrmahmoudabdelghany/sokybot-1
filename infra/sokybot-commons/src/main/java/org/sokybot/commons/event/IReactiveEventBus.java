package org.sokybot.commons.event;

import reactor.core.publisher.Flux;

/**
 * Interface for a type-safe, reactive event bus.
 * Allows components to subscribe to events using Project Reactor Flux.
 */
public interface IReactiveEventBus {

    /**
     * Returns a Flux of events of the specified type.
     * 
     * @param <T>       The event type
     * @param eventType The class of the event type to subscribe to
     * @return A Flux that emits events of the specified type
     */
    <T> Flux<T> on(Class<T> eventType);

    /**
     * Publishes an event to the bus.
     * 
     * @param event The event to publish
     */
    void publish(Object event);
}
