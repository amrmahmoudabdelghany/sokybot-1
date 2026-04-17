package org.sokybot.engine.api.event;

import org.sokybot.engine.api.EngineEvent;

public final class Connect extends EngineEvent {
    public static final Connect INSTANCE = new Connect();

    public Connect() {
        super("CONNECT");
    }
}
