package org.sokybot.gamemodel.spi;

import org.sokybot.gameevents.events.core.IGameEvent;

public interface IGameModelMutator {

    void dispatchGameEvent(IGameEvent event);
}
