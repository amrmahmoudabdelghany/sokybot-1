package org.sokybot.api.events;

/**
 * Event emitted when party state changes (opcode 0x3864).
 * Update types: Dismissed, Joined, Leave, Member, Leader, LeaderChange.
 */
public class PartyUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final byte updateType;
    private final Integer memberUniqueId;
    private final String memberName;
    private final Byte memberLevel;
    private final Byte memberHealthMana;  // 0-A|0-A format (HP%|MP%)
    
    public PartyUpdateEvent(String fullName, byte updateType, Integer memberUniqueId,
            String memberName, Byte memberLevel, Byte memberHealthMana) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.updateType = updateType;
        this.memberUniqueId = memberUniqueId;
        this.memberName = memberName;
        this.memberLevel = memberLevel;
        this.memberHealthMana = memberHealthMana;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public byte getUpdateType() { return updateType; }
    public Integer getMemberUniqueId() { return memberUniqueId; }
    public String getMemberName() { return memberName; }
    public Byte getMemberLevel() { return memberLevel; }
    public Byte getMemberHealthMana() { return memberHealthMana; }
    
    // Update type constants (from RSBot PartyUpdateType enum)
    public static final byte TYPE_DISMISSED = 0x01;
    public static final byte TYPE_JOINED = 0x02;
    public static final byte TYPE_LEAVE = 0x03;
    public static final byte TYPE_MEMBER = 0x04;
    public static final byte TYPE_LEADER = 0x05;
    public static final byte TYPE_LEADER_CHANGE = 0x06;
    
    public boolean isDismissed() { return updateType == TYPE_DISMISSED; }
    public boolean isJoined() { return updateType == TYPE_JOINED; }
    public boolean isLeave() { return updateType == TYPE_LEAVE; }
    
    @Override
    public String toString() {
        return "PartyUpdateEvent{updateType=" + updateType + 
               ", memberUniqueId=" + memberUniqueId +
               ", memberName=" + memberName + "}";
    }
}
