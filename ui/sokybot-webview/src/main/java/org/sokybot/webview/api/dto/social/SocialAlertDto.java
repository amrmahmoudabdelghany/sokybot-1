package org.sokybot.webview.api.dto.social;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire DTO mirroring {@link org.sokybot.social.api.SocialAlert}; {@code kind} is the enum name string.
 */
public final class SocialAlertDto {

    private final String machineId;
    private final long timestampEpochMs;
    private final String kind;
    private final String subject;
    private final Map<String, String> attributes;

    @JsonCreator
    public SocialAlertDto(
            @JsonProperty("machineId") String machineId,
            @JsonProperty("timestampEpochMs") long timestampEpochMs,
            @JsonProperty("kind") String kind,
            @JsonProperty("subject") String subject,
            @JsonProperty("attributes") Map<String, String> attributes) {
        this.machineId = machineId;
        this.timestampEpochMs = timestampEpochMs;
        this.kind = kind;
        this.subject = subject;
        this.attributes = attributes != null ? Map.copyOf(attributes) : Collections.emptyMap();
    }

    public String getMachineId() {
        return machineId;
    }

    public long getTimestampEpochMs() {
        return timestampEpochMs;
    }

    public String getKind() {
        return kind;
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
