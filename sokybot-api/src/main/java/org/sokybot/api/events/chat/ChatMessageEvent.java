package org.sokybot.api.events;

/**
 * Event fired when a chat message is received or sent.
 * Based on CommandHandler.sendChat() pattern.
 */
public class ChatMessageEvent implements IGameEvent {
    
    public enum ChatType {
        ALL,
        PARTY,
        GUILD,
        UNION,
        PRIVATE,
        STALL,
        GLOBAL,
        ACADEMY,
        NOTICE,
        UNKNOWN
    }
    
    private final String fullName;
    private final long timestamp;
    private final ChatType chatType;
    private final String senderName;
    private final String message;
    
    public ChatMessageEvent(String machineFullName, ChatType chatType, 
                           String senderName, String message) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.chatType = chatType;
        this.senderName = senderName;
        this.message = message;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public ChatType getChatType() { return chatType; }
    public String getSenderName() { return senderName; }
    public String getMessage() { return message; }
}
