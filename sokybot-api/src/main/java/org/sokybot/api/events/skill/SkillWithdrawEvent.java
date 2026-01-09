package org.sokybot.api.events;

/**
 * Event fired when a skill is withdrawn/downgraded (opcode 0xB202).
 */
public class SkillWithdrawEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final int oldSkillId;
    private final int newSkillId;
    
    public SkillWithdrawEvent(String fullName, boolean success, int oldSkillId, int newSkillId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.oldSkillId = oldSkillId;
        this.newSkillId = newSkillId;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public int getOldSkillId() { return oldSkillId; }
    public int getNewSkillId() { return newSkillId; }
    
    @Override
    public String toString() {
        return String.format("SkillWithdrawEvent[%s, success=%b, old=%d, new=%d]", 
            fullName, success, oldSkillId, newSkillId);
    }
}
