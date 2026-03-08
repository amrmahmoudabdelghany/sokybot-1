package org.sokybot.gameevents.events.session;

import org.sokybot.gameevents.events.core.IGameEvent;

public class LoginRequestEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final String username;
    private final String password;
    
    public LoginRequestEvent(String machineFullName, String username, String password) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.username = username;
        this.password = password;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
