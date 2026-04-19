package org.sokybot.login.settings.builtin;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.engine.api.login.ILoginSettingsSnapshotter;
import org.sokybot.engine.api.login.LoginSettingsSnapshot;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;

@Component(service = ILoginSettingsSnapshotter.class, immediate = true)
public final class LoginSettingsSnapshotterImpl implements ILoginSettingsSnapshotter {

    private static final Logger log = LoggerFactory.getLogger(LoginSettingsSnapshotterImpl.class);

    private static final String KEY_RUNTIME_LOGIN_SETTINGS = "runtimeLoginSettings";
    private static final String LOGIN_SCOPE = "login";

    @Override
    public LoginSettingsSnapshot snapshot(IWorkflowContext context, boolean forceRefresh) {
        if (context == null) {
            return defaults();
        }
        Object existing = context.getPersistentData().get(KEY_RUNTIME_LOGIN_SETTINGS);
        if (!forceRefresh && existing instanceof LoginSettingsSnapshot) {
            return (LoginSettingsSnapshot) existing;
        }
        Object source = readSettingsSource(context);
        LoginSettingsSnapshot computed = fromSource(source);
        context.getPersistentData().put(KEY_RUNTIME_LOGIN_SETTINGS, computed);
        return computed;
    }

    private Object readSettingsSource(IWorkflowContext context) {
        ISettingsRegistry registry = context.getServiceOptional(ISettingsRegistry.class).orElse(null);
        if (registry == null) {
            return null;
        }
        try {
            Class<?> loginSettingsType = resolveLoginSettingsType(registry);
            if (loginSettingsType != null) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                ISettingsProvider<?> provider = ((ISettingsRegistry) registry).getProvider(
                        context.getGroupName(),
                        context.getMachineName(),
                        LOGIN_SCOPE,
                        (Class) loginSettingsType
                );
                if (provider != null) {
                    return provider.get();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to read typed login settings provider: {}", e.getMessage());
        }
        try {
            return registry.readRawSettings(context.getGroupName(), context.getMachineName(), LOGIN_SCOPE);
        } catch (Exception e) {
            log.debug("Failed to read raw login settings: {}", e.getMessage());
            return null;
        }
    }

    private static Class<?> resolveLoginSettingsType(ISettingsRegistry registry) {
        if (registry != null) {
            Class<?> registered = registry.getRegisteredSettingsType(LOGIN_SCOPE);
            if (registered != null) {
                return registered;
            }
        }
        try {
            return Class.forName("LoginSettings");
        } catch (Exception ignored) {
            return null;
        }
    }

    private LoginSettingsSnapshot fromSource(Object source) {
        if (source == null) {
            return defaults();
        }

        String targetGateway = SettingsValueReader.asString(source, "targetGateway", "");
        String username = SettingsValueReader.asString(source, "username", "");
        String password = SettingsValueReader.asString(source, "password", "");
        String passcode = SettingsValueReader.asString(source, "passcode", "");
        String targetAgent = SettingsValueReader.asString(source, "targetAgent", "");
        String selectedCharacter = SettingsValueReader.asString(source, "selectedCharacter", "");
        int locale = effectiveGatewayLocale(source);
        boolean autoLogin = SettingsValueReader.asBoolean(source, "autoLogin", false);
        boolean autoReconnect = SettingsValueReader.asBoolean(source, "autoReconnect", true);
        String loginCharset = SettingsValueReader.asString(source, "loginCharset", "windows-1252");
        int gatewayClientVersion = SettingsValueReader.asInt(source, "gatewayClientVersion", 188);
        String gatewayClientModule = normalizeGatewayClientModule(
                SettingsValueReader.asString(source, "gatewayClientModule", "SR_Client")
        );
        int selectedCharacterSlot = SettingsValueReader.asInt(source, "selectedCharacterSlot", -1);
        int characterSlotBase = SettingsValueReader.asInt(source, "characterSlotBase", 0);
        boolean strictCharacterSelection = SettingsValueReader.asBoolean(source, "characterSelectionStrictMode", false);
        boolean passcodeStringDetectionEnabled = SettingsValueReader.asBoolean(source, "passcodeStringDetectionEnabled", true);
        long retryBaseDelayMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "retryBaseDelayMs", 5000L), 5000L, 500L, 300000L
        );
        long retryMaxDelayMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "retryMaxDelayMs", 60000L), 60000L, 500L, 300000L
        );
        int maxRetryAttempts = Math.max(0, SettingsValueReader.asInt(source, "maxRetryAttempts", 0));
        boolean infiniteRetryMode = SettingsValueReader.asBoolean(source, "infiniteRetryMode", true);
        long loginResponseTimeoutMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "loginResponseTimeoutMs", 15000L), 15000L, 15000L, 30000L
        );
        long agentWaitTimeoutMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "agentWaitTimeoutMs", 15000L), 15000L, 5000L, 30000L
        );
        long agentAuthTimeoutMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "agentAuthTimeoutMs", 30000L), 30000L, 10000L, 45000L
        );
        long passcodeWaitTimeoutMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "passcodeWaitTimeoutMs", 60000L), 60000L, 5000L, 120000L
        );
        long passcodeUserInputTimeoutMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "passcodeUserInputTimeoutMs", 60000L), 60000L, 5000L, 180000L
        );
        int agentRequestMaxRetries = Math.max(0, SettingsValueReader.asInt(source, "agentRequestMaxRetries", 2));
        long agentRequestRetryBackoffMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "agentRequestRetryBackoffMs", 2000L), 2000L, 500L, 60000L
        );
        long agentBanBlockDurationMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "agentBanBlockDurationMs", 300000L), 300000L, 10000L, 3600000L
        );
        long gatewayLoginMinIntervalMs = Math.max(0L, SettingsValueReader.asLong(source, "gatewayLoginMinIntervalMs", 2000L));
        long gatewayLoginPauseAfterAgentListMs =
                Math.max(0L, SettingsValueReader.asLong(source, "gatewayLoginPauseAfterAgentListMs", 1500L));
        long logoutAckTimeoutMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "logoutAckTimeoutMs", 3000L), 3000L, 500L, 10000L
        );
        boolean alertOnImageCaptcha = SettingsValueReader.asBoolean(source, "alertOnImageCaptcha", false);
        long captchaSolveTimeoutMs = LoginTimeoutNormalizer.normalize(
                SettingsValueReader.asLong(source, "captchaSolveTimeoutMs", 60000L), 60000L, 5000L, 300000L
        );
        boolean stopBotOnCaptchaUnsolved = SettingsValueReader.asBoolean(source, "stopBotOnCaptchaUnsolved", false);

        LoginTimeoutNormalizer.Normalized normalized = LoginTimeoutNormalizer.normalized(
                agentWaitTimeoutMs, loginResponseTimeoutMs, agentRequestMaxRetries, agentRequestRetryBackoffMs
        );

        return new LoginSettingsSnapshot(
                targetGateway,
                username,
                password,
                passcode,
                targetAgent,
                selectedCharacter,
                locale,
                autoLogin,
                autoReconnect,
                loginCharset,
                gatewayClientVersion,
                gatewayClientModule,
                selectedCharacterSlot,
                characterSlotBase,
                strictCharacterSelection,
                passcodeStringDetectionEnabled,
                retryBaseDelayMs,
                retryMaxDelayMs,
                maxRetryAttempts,
                infiniteRetryMode,
                normalized.getAgentWaitTimeoutMs(),
                loginResponseTimeoutMs,
                agentAuthTimeoutMs,
                passcodeWaitTimeoutMs,
                passcodeUserInputTimeoutMs,
                normalized.getAgentRequestMaxRetries(),
                agentRequestRetryBackoffMs,
                agentBanBlockDurationMs,
                gatewayLoginMinIntervalMs,
                gatewayLoginPauseAfterAgentListMs,
                logoutAckTimeoutMs,
                alertOnImageCaptcha,
                captchaSolveTimeoutMs,
                stopBotOnCaptchaUnsolved
        );
    }

    private static LoginSettingsSnapshot defaults() {
        return new LoginSettingsSnapshot(
                "",
                "",
                "",
                "",
                "",
                "",
                22,
                false,
                true,
                "windows-1252",
                188,
                "SR_Client",
                -1,
                0,
                false,
                true,
                5000L,
                60000L,
                0,
                true,
                15000L,
                15000L,
                30000L,
                60000L,
                60000L,
                2,
                2000L,
                300000L,
                2000L,
                1500L,
                3000L,
                false,
                60000L,
                false
        );
    }

    private static String normalizeGatewayClientModule(String value) {
        String module = value != null ? value.trim() : "";
        return module.isEmpty() ? "SR_Client" : module;
    }

    private static int effectiveGatewayLocale(Object source) {
        int locale = SettingsValueReader.asInt(source, "locale", 22);
        return locale == 0 ? 22 : (locale & 0xFF);
    }
}
