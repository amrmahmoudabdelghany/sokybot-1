package org.sokybot.engine.api;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class EngineEvent {
    public static final EngineEvent START_TRAINING = new EngineEvent("START_TRAINING");
    public static final EngineEvent STOP_TRAINING = new EngineEvent("STOP_TRAINING");
    public static final EngineEvent CONNECT = new EngineEvent("CONNECT");
    public static final EngineEvent DISCONNECT = new EngineEvent("DISCONNECT");

    private final String type;
    private final Map<String, Object> payload;

    public EngineEvent(String type) {
        this(type, Collections.emptyMap());
    }

    public EngineEvent(String type, Map<String, Object> payload) {
        this.type = Objects.requireNonNull(type, "type");
        this.payload = payload == null ? Collections.emptyMap() : Collections.unmodifiableMap(payload);
    }

    public String type() {
        return type;
    }

    public Map<String, Object> payload() {
        return payload;
    }
}
