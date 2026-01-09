package org.sokybot.api.events;

/**
 * Event fired when login response is received.
 * Based on ServerOpcode.LOGIN_RESPONSE (0xA102).
 */
public class LoginResponseEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final byte resultCode; // Result code for debugging
    
    public LoginResponseEvent(String machineFullName, boolean success, byte resultCode) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.resultCode = resultCode;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public byte getResultCode() { return resultCode; }
}
