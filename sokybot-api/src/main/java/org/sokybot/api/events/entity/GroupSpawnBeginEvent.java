package org.sokybot.api.events;

/**
 * Event fired when group spawn batch begins.
 * Contains the spawn type and expected count.
 */
public class GroupSpawnBeginEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final byte spawnType;  // 1 = spawn, 2 = despawn
    private final short count;
    
    public GroupSpawnBeginEvent(String machineFullName, byte spawnType, short count) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.spawnType = spawnType;
        this.count = count;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public byte getSpawnType() { return spawnType; }
    public short getCount() { return count; }
    public boolean isSpawn() { return spawnType == 1; }
    public boolean isDespawn() { return spawnType == 2; }
}
