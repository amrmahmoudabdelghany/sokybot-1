package org.sokybot.engine.api.login;

import java.util.Objects;

/**
 * Immutable, normalized runtime snapshot of login settings.
 */
public final class LoginSettingsSnapshot {

    private final String targetGateway;
    private final String username;
    private final String password;
    private final String passcode;
    private final String targetAgent;
    private final String selectedCharacter;
    private final int locale;
    private final boolean autoLogin;
    private final boolean autoReconnect;
    private final String loginCharset;
    private final int gatewayClientVersion;
    private final String gatewayClientModule;
    private final int selectedCharacterSlot;
    private final int characterSlotBase;
    private final boolean characterSelectionStrictMode;
    private final boolean passcodeStringDetectionEnabled;
    private final long retryBaseDelayMs;
    private final long retryMaxDelayMs;
    private final int maxRetryAttempts;
    private final boolean infiniteRetryMode;
    private final long agentWaitTimeoutMs;
    private final long loginResponseTimeoutMs;
    private final long agentAuthTimeoutMs;
    private final long passcodeWaitTimeoutMs;
    private final long passcodeUserInputTimeoutMs;
    private final int agentRequestMaxRetries;
    private final long agentRequestRetryBackoffMs;
    private final long agentBanBlockDurationMs;
    private final long gatewayLoginMinIntervalMs;
    private final long gatewayLoginPauseAfterAgentListMs;
    private final long logoutAckTimeoutMs;
    private final boolean alertOnImageCaptcha;
    private final long captchaSolveTimeoutMs;
    private final boolean stopBotOnCaptchaUnsolved;

    public LoginSettingsSnapshot(
            String targetGateway,
            String username,
            String password,
            String passcode,
            String targetAgent,
            String selectedCharacter,
            int locale,
            boolean autoLogin,
            boolean autoReconnect,
            String loginCharset,
            int gatewayClientVersion,
            String gatewayClientModule,
            int selectedCharacterSlot,
            int characterSlotBase,
            boolean characterSelectionStrictMode,
            boolean passcodeStringDetectionEnabled,
            long retryBaseDelayMs,
            long retryMaxDelayMs,
            int maxRetryAttempts,
            boolean infiniteRetryMode,
            long agentWaitTimeoutMs,
            long loginResponseTimeoutMs,
            long agentAuthTimeoutMs,
            long passcodeWaitTimeoutMs,
            long passcodeUserInputTimeoutMs,
            int agentRequestMaxRetries,
            long agentRequestRetryBackoffMs,
            long agentBanBlockDurationMs,
            long gatewayLoginMinIntervalMs,
            long gatewayLoginPauseAfterAgentListMs,
            long logoutAckTimeoutMs,
            boolean alertOnImageCaptcha,
            long captchaSolveTimeoutMs,
            boolean stopBotOnCaptchaUnsolved
    ) {
        this.targetGateway = targetGateway != null ? targetGateway : "";
        this.username = username != null ? username : "";
        this.password = password != null ? password : "";
        this.passcode = passcode != null ? passcode : "";
        this.targetAgent = targetAgent != null ? targetAgent : "";
        this.selectedCharacter = selectedCharacter != null ? selectedCharacter : "";
        this.locale = locale;
        this.autoLogin = autoLogin;
        this.autoReconnect = autoReconnect;
        this.loginCharset = loginCharset != null ? loginCharset : "windows-1252";
        this.gatewayClientVersion = gatewayClientVersion;
        this.gatewayClientModule = gatewayClientModule != null ? gatewayClientModule : "SR_Client";
        this.selectedCharacterSlot = selectedCharacterSlot;
        this.characterSlotBase = characterSlotBase;
        this.characterSelectionStrictMode = characterSelectionStrictMode;
        this.passcodeStringDetectionEnabled = passcodeStringDetectionEnabled;
        this.retryBaseDelayMs = retryBaseDelayMs;
        this.retryMaxDelayMs = retryMaxDelayMs;
        this.maxRetryAttempts = maxRetryAttempts;
        this.infiniteRetryMode = infiniteRetryMode;
        this.agentWaitTimeoutMs = agentWaitTimeoutMs;
        this.loginResponseTimeoutMs = loginResponseTimeoutMs;
        this.agentAuthTimeoutMs = agentAuthTimeoutMs;
        this.passcodeWaitTimeoutMs = passcodeWaitTimeoutMs;
        this.passcodeUserInputTimeoutMs = passcodeUserInputTimeoutMs;
        this.agentRequestMaxRetries = agentRequestMaxRetries;
        this.agentRequestRetryBackoffMs = agentRequestRetryBackoffMs;
        this.agentBanBlockDurationMs = agentBanBlockDurationMs;
        this.gatewayLoginMinIntervalMs = gatewayLoginMinIntervalMs;
        this.gatewayLoginPauseAfterAgentListMs = gatewayLoginPauseAfterAgentListMs;
        this.logoutAckTimeoutMs = logoutAckTimeoutMs;
        this.alertOnImageCaptcha = alertOnImageCaptcha;
        this.captchaSolveTimeoutMs = captchaSolveTimeoutMs;
        this.stopBotOnCaptchaUnsolved = stopBotOnCaptchaUnsolved;
    }

    public String getTargetGateway() { return targetGateway; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getPasscode() { return passcode; }
    public String getTargetAgent() { return targetAgent; }
    public String getSelectedCharacter() { return selectedCharacter; }
    public int getLocale() { return locale; }
    public boolean isAutoLogin() { return autoLogin; }
    public boolean isAutoReconnect() { return autoReconnect; }
    public String getLoginCharset() { return loginCharset; }
    public int getGatewayClientVersion() { return gatewayClientVersion; }
    public String getGatewayClientModule() { return gatewayClientModule; }
    public int getSelectedCharacterSlot() { return selectedCharacterSlot; }
    public int getCharacterSlotBase() { return characterSlotBase; }
    public boolean isCharacterSelectionStrictMode() { return characterSelectionStrictMode; }
    public boolean isPasscodeStringDetectionEnabled() { return passcodeStringDetectionEnabled; }
    public long getRetryBaseDelayMs() { return retryBaseDelayMs; }
    public long getRetryMaxDelayMs() { return retryMaxDelayMs; }
    public int getMaxRetryAttempts() { return maxRetryAttempts; }
    public boolean isInfiniteRetryMode() { return infiniteRetryMode; }
    public long getAgentWaitTimeoutMs() { return agentWaitTimeoutMs; }
    public long getLoginResponseTimeoutMs() { return loginResponseTimeoutMs; }
    public long getAgentAuthTimeoutMs() { return agentAuthTimeoutMs; }
    public long getPasscodeWaitTimeoutMs() { return passcodeWaitTimeoutMs; }
    public long getPasscodeUserInputTimeoutMs() { return passcodeUserInputTimeoutMs; }
    public int getAgentRequestMaxRetries() { return agentRequestMaxRetries; }
    public long getAgentRequestRetryBackoffMs() { return agentRequestRetryBackoffMs; }
    public long getAgentBanBlockDurationMs() { return agentBanBlockDurationMs; }
    public long getGatewayLoginMinIntervalMs() { return gatewayLoginMinIntervalMs; }
    public long getGatewayLoginPauseAfterAgentListMs() { return gatewayLoginPauseAfterAgentListMs; }
    public long getLogoutAckTimeoutMs() { return logoutAckTimeoutMs; }
    public boolean isAlertOnImageCaptcha() { return alertOnImageCaptcha; }
    public long getCaptchaSolveTimeoutMs() { return captchaSolveTimeoutMs; }
    public boolean isStopBotOnCaptchaUnsolved() { return stopBotOnCaptchaUnsolved; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoginSettingsSnapshot)) {
            return false;
        }
        LoginSettingsSnapshot that = (LoginSettingsSnapshot) o;
        return autoLogin == that.autoLogin
                && autoReconnect == that.autoReconnect
                && gatewayClientVersion == that.gatewayClientVersion
                && selectedCharacterSlot == that.selectedCharacterSlot
                && characterSlotBase == that.characterSlotBase
                && characterSelectionStrictMode == that.characterSelectionStrictMode
                && passcodeStringDetectionEnabled == that.passcodeStringDetectionEnabled
                && retryBaseDelayMs == that.retryBaseDelayMs
                && retryMaxDelayMs == that.retryMaxDelayMs
                && maxRetryAttempts == that.maxRetryAttempts
                && infiniteRetryMode == that.infiniteRetryMode
                && agentWaitTimeoutMs == that.agentWaitTimeoutMs
                && loginResponseTimeoutMs == that.loginResponseTimeoutMs
                && agentAuthTimeoutMs == that.agentAuthTimeoutMs
                && passcodeWaitTimeoutMs == that.passcodeWaitTimeoutMs
                && passcodeUserInputTimeoutMs == that.passcodeUserInputTimeoutMs
                && agentRequestMaxRetries == that.agentRequestMaxRetries
                && agentRequestRetryBackoffMs == that.agentRequestRetryBackoffMs
                && agentBanBlockDurationMs == that.agentBanBlockDurationMs
                && gatewayLoginMinIntervalMs == that.gatewayLoginMinIntervalMs
                && gatewayLoginPauseAfterAgentListMs == that.gatewayLoginPauseAfterAgentListMs
                && logoutAckTimeoutMs == that.logoutAckTimeoutMs
                && Objects.equals(targetGateway, that.targetGateway)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(passcode, that.passcode)
                && Objects.equals(targetAgent, that.targetAgent)
                && Objects.equals(selectedCharacter, that.selectedCharacter)
                && locale == that.locale
                && Objects.equals(loginCharset, that.loginCharset)
                && Objects.equals(gatewayClientModule, that.gatewayClientModule)
                && alertOnImageCaptcha == that.alertOnImageCaptcha
                && captchaSolveTimeoutMs == that.captchaSolveTimeoutMs
                && stopBotOnCaptchaUnsolved == that.stopBotOnCaptchaUnsolved;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                targetGateway, username, password, passcode, targetAgent, selectedCharacter, locale,
                autoLogin, autoReconnect, loginCharset, gatewayClientVersion, gatewayClientModule,
                selectedCharacterSlot, characterSlotBase, characterSelectionStrictMode,
                passcodeStringDetectionEnabled, retryBaseDelayMs, retryMaxDelayMs, maxRetryAttempts,
                infiniteRetryMode, agentWaitTimeoutMs, loginResponseTimeoutMs, agentAuthTimeoutMs,
                passcodeWaitTimeoutMs, passcodeUserInputTimeoutMs, agentRequestMaxRetries,
                agentRequestRetryBackoffMs, agentBanBlockDurationMs, gatewayLoginMinIntervalMs,
                gatewayLoginPauseAfterAgentListMs, logoutAckTimeoutMs,
                alertOnImageCaptcha, captchaSolveTimeoutMs, stopBotOnCaptchaUnsolved
        );
    }

    @Override
    public String toString() {
        return "LoginSettingsSnapshot{"
                + "targetGateway='" + targetGateway + '\''
                + ", username='" + username + '\''
                + ", password=<redacted>"
                + ", passcode=<redacted>"
                + ", targetAgent='" + targetAgent + '\''
                + ", selectedCharacter='" + selectedCharacter + '\''
                + ", locale=" + locale
                + ", autoLogin=" + autoLogin
                + ", autoReconnect=" + autoReconnect
                + ", loginCharset='" + loginCharset + '\''
                + ", gatewayClientVersion=" + gatewayClientVersion
                + ", gatewayClientModule='" + gatewayClientModule + '\''
                + ", selectedCharacterSlot=" + selectedCharacterSlot
                + ", characterSlotBase=" + characterSlotBase
                + ", characterSelectionStrictMode=" + characterSelectionStrictMode
                + ", passcodeStringDetectionEnabled=" + passcodeStringDetectionEnabled
                + ", retryBaseDelayMs=" + retryBaseDelayMs
                + ", retryMaxDelayMs=" + retryMaxDelayMs
                + ", maxRetryAttempts=" + maxRetryAttempts
                + ", infiniteRetryMode=" + infiniteRetryMode
                + ", agentWaitTimeoutMs=" + agentWaitTimeoutMs
                + ", loginResponseTimeoutMs=" + loginResponseTimeoutMs
                + ", agentAuthTimeoutMs=" + agentAuthTimeoutMs
                + ", passcodeWaitTimeoutMs=" + passcodeWaitTimeoutMs
                + ", passcodeUserInputTimeoutMs=" + passcodeUserInputTimeoutMs
                + ", agentRequestMaxRetries=" + agentRequestMaxRetries
                + ", agentRequestRetryBackoffMs=" + agentRequestRetryBackoffMs
                + ", agentBanBlockDurationMs=" + agentBanBlockDurationMs
                + ", gatewayLoginMinIntervalMs=" + gatewayLoginMinIntervalMs
                + ", gatewayLoginPauseAfterAgentListMs=" + gatewayLoginPauseAfterAgentListMs
                + ", logoutAckTimeoutMs=" + logoutAckTimeoutMs
                + ", alertOnImageCaptcha=" + alertOnImageCaptcha
                + ", captchaSolveTimeoutMs=" + captchaSolveTimeoutMs
                + ", stopBotOnCaptchaUnsolved=" + stopBotOnCaptchaUnsolved
                + '}';
    }
}
