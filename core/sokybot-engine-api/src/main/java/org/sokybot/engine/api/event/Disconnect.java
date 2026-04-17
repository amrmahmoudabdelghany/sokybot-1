package org.sokybot.engine.api.event;

import org.sokybot.engine.api.EngineEvent;

public final class Disconnect extends EngineEvent {
    public static final Disconnect INSTANCE = new Disconnect();

    public Disconnect() {
        super("DISCONNECT");
    }
}
