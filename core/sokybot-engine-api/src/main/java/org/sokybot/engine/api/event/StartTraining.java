package org.sokybot.engine.api.event;

import org.sokybot.engine.api.EngineEvent;

public final class StartTraining extends EngineEvent {
    public static final StartTraining INSTANCE = new StartTraining();

    public StartTraining() {
        super("START_TRAINING");
    }
}
