package org.sokybot.api.events;

/**
 * Event emitted when player talks to an NPC (opcode 0xB046).
 */
public class NpcTalkEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final byte talkOption;  // Trade, Talk, etc.
    private final int npcUniqueId;
    
    public NpcTalkEvent(String fullName, byte talkOption, int npcUniqueId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.talkOption = talkOption;
        this.npcUniqueId = npcUniqueId;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public byte getTalkOption() { return talkOption; }
    public int getNpcUniqueId() { return npcUniqueId; }
    
    // Talk option constants
    public static final byte OPTION_TALK = 0x01;
    public static final byte OPTION_TRADE = 0x02;
    public static final byte OPTION_REPAIR = 0x03;
    
    public boolean isTrade() { return talkOption == OPTION_TRADE; }
    
    @Override
    public String toString() {
        return "NpcTalkEvent{talkOption=" + talkOption + ", npcUniqueId=" + npcUniqueId + "}";
    }
}
