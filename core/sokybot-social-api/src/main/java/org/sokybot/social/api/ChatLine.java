package org.sokybot.social.api;

public final class ChatLine {
    private final String machineId;
    private final long timestampEpochMs;
    private final SocialChannel channel;
    private final String senderName;
    private final String message;
    private final boolean fromGameMaster;
    private final boolean fromSelf;

    public ChatLine(String machineId, long timestampEpochMs, SocialChannel channel, 
                    String senderName, String message, boolean fromGameMaster, boolean fromSelf) {
        this.machineId = machineId;
        this.timestampEpochMs = timestampEpochMs;
        this.channel = channel;
        this.senderName = senderName;
        this.message = message;
        this.fromGameMaster = fromGameMaster;
        this.fromSelf = fromSelf;
    }

    public String getMachineId() { return machineId; }
    public long getTimestampEpochMs() { return timestampEpochMs; }
    public SocialChannel getChannel() { return channel; }
    public String getSenderName() { return senderName; }
    public String getMessage() { return message; }
    public boolean isFromGameMaster() { return fromGameMaster; }
    public boolean isFromSelf() { return fromSelf; }
}
