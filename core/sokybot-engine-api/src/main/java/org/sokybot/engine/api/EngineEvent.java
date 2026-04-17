package org.sokybot.engine.api;

import org.sokybot.engine.api.event.Connect;
import org.sokybot.engine.api.event.Disconnect;
import org.sokybot.engine.api.event.StartTraining;
import org.sokybot.engine.api.event.StopTraining;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public abstract class EngineEvent {
    @Deprecated
    public static final EngineEvent START_TRAINING = new StartTraining();
    @Deprecated
    public static final EngineEvent STOP_TRAINING = new StopTraining();
    @Deprecated
    public static final EngineEvent CONNECT = new Connect();
    @Deprecated
    public static final EngineEvent DISCONNECT = new Disconnect();

    private final String type;

    protected EngineEvent(String type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public String type() {
        return type;
    }

    public Map<String, Object> payload() {
        return Collections.emptyMap();
    }
}
