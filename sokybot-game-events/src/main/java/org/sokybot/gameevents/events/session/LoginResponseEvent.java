package org.sokybot.gameevents.events.session;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when login response is received.
 * Based on ServerOpcode.LOGIN_RESPONSE (0xA102).
 */
public class LoginResponseEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final byte resultCode; // Result code for debugging
    private final int loginId;
    private final String agentHost;
    private final int agentPort;
    
    public LoginResponseEvent(String machineFullName, boolean success, byte resultCode, int loginId, String agentHost, int agentPort) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.resultCode = resultCode;
        this.loginId = loginId;
        this.agentHost = agentHost;
        this.agentPort = agentPort;
    }
    
    public LoginResponseEvent(String machineFullName, boolean success, byte resultCode) {
        this(machineFullName, success, resultCode, 0, null, 0);
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
    public int getLoginId() { return loginId; }
    public String getAgentHost() { return agentHost; }
    public int getAgentPort() { return agentPort; }
}
