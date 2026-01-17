package org.sokybot.actuator.login;

import lombok.Data;
import org.sokybot.settings.security.Encrypted;

/**
 * Settings for login actuator.
 */
@Data
public class LoginSettings {
    
    private String targetGateway = "";
    
    @Encrypted
    private String username = "";
    
    @Encrypted
    private String password = "";
    
    @Encrypted
    private String passcode = "";
    
    private String targetAgent = "";
    private boolean autoLogin = false;
}
