package org.sokybot.engine.api.event;

import org.sokybot.engine.api.EngineEvent;

public final class Wake extends EngineEvent {
    public static final Wake INSTANCE = new Wake();

    private Wake() {
        super("WAKE");
    }
}
