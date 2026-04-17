package org.sokybot.engine.api.handler;

import org.sokybot.engine.api.EngineEvent;

/**
 * Strategy contract for handling a single EngineEvent type.
 */
public interface IEngineEventHandler<E extends EngineEvent> {

    Class<E> eventType();

    void handle(E event, IEngineRuntime runtime);

    default int getRanking() {
        return 0;
    }
}
