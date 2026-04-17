package org.sokybot.gamemodel;

import org.sokybot.gameevents.events.core.IGameEvent;

public interface IGameModel extends IGameModelQuery, IGameModelObserve {

    /**
     * Apply a translated game event for this machine (gateway/login/session). Prefer this path over the
     * reactive bus so the model updates in the same thread as the translator, before any engine work runs.
     */
    @Deprecated
    default void dispatchGameEvent(IGameEvent event) {
        throw new UnsupportedOperationException(
                "dispatchGameEvent moved to org.sokybot.gamemodel.spi.IGameModelMutator");
    }
}
