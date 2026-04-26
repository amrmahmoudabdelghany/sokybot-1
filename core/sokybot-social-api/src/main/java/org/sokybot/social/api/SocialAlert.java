package org.sokybot.social.api;

import java.util.Map;
import java.util.Collections;

public final class SocialAlert {

    public enum Kind {
        UNIQUE_SPAWNED,
        UNIQUE_KILLED,
        HIVE_DISPATCHED,
        HIVE_COMPLETED,
        HIVE_ABORTED,
        GM_NEARBY,
        GM_WHISPER,
        NOTICE_GM_BROADCAST,
        CUSTOM
    }

    private final String machineId;
    private final long timestampEpochMs;
    private final Kind kind;
    private final String subject;
    private final Map<String, String> attributes;

    public SocialAlert(String machineId, long timestampEpochMs, Kind kind, String subject, Map<String, String> attributes) {
        this.machineId = machineId;
        this.timestampEpochMs = timestampEpochMs;
        this.kind = kind;
        this.subject = subject;
        this.attributes = attributes != null ? Map.copyOf(attributes) : Collections.emptyMap();
    }

    public String getMachineId() { return machineId; }
    public long getTimestampEpochMs() { return timestampEpochMs; }
    public Kind getKind() { return kind; }
    public String getSubject() { return subject; }
    public Map<String, String> getAttributes() { return attributes; }
}
