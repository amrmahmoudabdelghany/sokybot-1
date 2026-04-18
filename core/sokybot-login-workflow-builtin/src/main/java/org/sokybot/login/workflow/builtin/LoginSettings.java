package org.sokybot.login.workflow.builtin;

import org.sokybot.settings.security.Encrypted;

/**
 * Durable login settings model for scope {@code login} (persisted via {@link org.sokybot.settings.api.ISettingsRegistry}).
 */
public final class LoginSettings {

    public String targetGateway = "";
    @Encrypted public String username = "";
    @Encrypted public String password = "";
    @Encrypted public String passcode = "";
    public String targetAgent = "";
    public String selectedCharacter = "";
    public int locale = 22;
    public boolean autoLogin = false;
    public boolean autoReconnect = true;
    public int retryBaseDelayMs = 5000;
    public int retryMaxDelayMs = 60000;
    public int maxRetryAttempts = 0;
    public boolean infiniteRetryMode = true;
    public String loginCharset = "windows-1252";
    public int agentWaitTimeoutMs = 15000;
    public int loginResponseTimeoutMs = 15000;
    public int agentAuthTimeoutMs = 30000;
    public int passcodeWaitTimeoutMs = 60000;
    public int passcodeUserInputTimeoutMs = 60000;
    public int agentRequestMaxRetries = 2;
    public int agentRequestRetryBackoffMs = 2000;
    public int agentBanBlockDurationMs = 300000;
    public int gatewayLoginMinIntervalMs = 2000;
    public int gatewayLoginPauseAfterAgentListMs = 1500;
    public int gatewayClientVersion = 188;
    public String gatewayClientModule = "SR_Client";
    public int selectedCharacterSlot = -1;
    public int characterSlotBase = 0;
    public boolean characterSelectionStrictMode = false;
    public boolean passcodeStringDetectionEnabled = true;
    public int logoutAckTimeoutMs = 3000;

    public LoginSettings() {
    }

    public boolean isPasscodeStringDetectionEnabled() {
        return passcodeStringDetectionEnabled;
    }
}
