package org.sokybot.gameevents.events.session;

import org.sokybot.gameevents.events.core.IGameEvent;

public class AuthResponseEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final byte resultCode;
    
    public AuthResponseEvent(String machineFullName, boolean success, byte resultCode) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.resultCode = resultCode;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public byte getResultCode() { return resultCode; }
}
