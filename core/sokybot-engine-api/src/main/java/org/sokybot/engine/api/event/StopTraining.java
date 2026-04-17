package org.sokybot.engine.api.event;

import org.sokybot.engine.api.EngineEvent;

public final class StopTraining extends EngineEvent {
    public static final StopTraining INSTANCE = new StopTraining();

    public StopTraining() {
        super("STOP_TRAINING");
    }
}
