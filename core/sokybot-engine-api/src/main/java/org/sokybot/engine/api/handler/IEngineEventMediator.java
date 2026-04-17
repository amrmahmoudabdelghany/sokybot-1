package org.sokybot.engine.api.handler;

import java.util.function.Consumer;

import org.sokybot.engine.api.EngineEvent;

/**
 * Typed mediator for engine events.
 */
public interface IEngineEventMediator {

    <E extends EngineEvent> void relay(E event);

    <E extends EngineEvent> Subscription subscribe(Class<E> eventType, Consumer<E> consumer);

    interface Subscription extends AutoCloseable {
        @Override
        void close();
    }
}
