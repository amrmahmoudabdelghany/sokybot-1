package org.sokybot.webview.api.dto.social;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire DTO mirroring {@link org.sokybot.social.api.ChatLine} for RSocket JSON payloads.
 */
public final class ChatLineDto {

    private final String machineId;
    private final long timestampEpochMs;
    private final String channel;
    private final String senderName;
    private final String message;
    private final boolean fromGameMaster;
    private final boolean fromSelf;

    @JsonCreator
    public ChatLineDto(
            @JsonProperty("machineId") String machineId,
            @JsonProperty("timestampEpochMs") long timestampEpochMs,
            @JsonProperty("channel") String channel,
            @JsonProperty("senderName") String senderName,
            @JsonProperty("message") String message,
            @JsonProperty("fromGameMaster") boolean fromGameMaster,
            @JsonProperty("fromSelf") boolean fromSelf) {
        this.machineId = machineId;
        this.timestampEpochMs = timestampEpochMs;
        this.channel = channel;
        this.senderName = senderName;
        this.message = message;
        this.fromGameMaster = fromGameMaster;
        this.fromSelf = fromSelf;
    }

    public String getMachineId() {
        return machineId;
    }

    public long getTimestampEpochMs() {
        return timestampEpochMs;
    }

    public String getChannel() {
        return channel;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFromGameMaster() {
        return fromGameMaster;
    }

    public boolean isFromSelf() {
        return fromSelf;
    }
}
