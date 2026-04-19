package org.sokybot.login.workflow.builtin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.engine.api.login.GatewayCredentials;
import org.sokybot.engine.api.login.IGatewayProtocolEmitter;
import org.sokybot.engine.api.login.ILoginFailureClassifier;
import org.sokybot.engine.api.login.ILoginInteractiveCoordinator;
import org.sokybot.engine.api.login.ILoginSettingsSnapshotter;
import org.sokybot.engine.api.login.InteractiveOutcome;
import org.sokybot.engine.api.login.LoginFailureClass;
import org.sokybot.engine.api.login.LoginFailureVerdict;
import org.sokybot.engine.api.login.LoginPhaseAliases;
import org.sokybot.engine.api.login.LoginSettingsSnapshot;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.AgentInfo;
import org.sokybot.gameevents.events.session.SessionConnectedEvent;
import org.sokybot.gameevents.events.session.SessionReconnectingEvent;
import org.sokybot.gamemodel.LoginState;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.commons.event.IReactiveEventBus;

/**
 * Port of the former {@code Login.groovy} helper logic: settings, gateway selection, telemetry, retry policy, and flow checks.
 */
final class LoginWorkflowSupport {

    private static final Logger log = LoggerFactory.getLogger(LoginWorkflowSupport.class);
    private static final long[] RETRY_DELAYS_MS = {5000L, 10000L, 30000L, 60000L};
    private static final Pattern QUEUE_POSITION = Pattern.compile("(?i)\\bposition\\s+(\\d+)\\b");

    private final LoginCycleDeps deps;

    LoginWorkflowSupport(LoginCycleDeps deps) {
        this.deps = deps;
    }

    static final class HostPort {
        final String host;
        final int port;

        HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    static final class FlowSnapshot {
        final LoginState loginState;
        final LoginState.Phase phase;
        final String failureReason;
        final boolean agentListReceived;
        final int agentListCount;
        final boolean serverConnected;
        final boolean loggedIn;
        final long nowMs;

        FlowSnapshot(
                LoginState loginState,
                LoginState.Phase phase,
                String failureReason,
                boolean agentListReceived,
                int agentListCount,
                boolean serverConnected,
                boolean loggedIn,
                long nowMs
        ) {
            this.loginState = loginState;
            this.phase = phase;
            this.failureReason = failureReason;
            this.agentListReceived = agentListReceived;
            this.agentListCount = agentListCount;
            this.serverConnected = serverConnected;
            this.loggedIn = loggedIn;
            this.nowMs = nowMs;
        }
    }

    FlowSnapshot snapshot(IWorkflowContext ctx) {
        LoginState loginState = null;
        LoginState.Phase phase = null;
        String failureReason = null;
        boolean agentListReceived = false;
        int agentListCount = 0;
        try {
            loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
            if (loginState != null) {
                phase = loginState.getPhase();
                failureReason = loginState.getFailureReason();
                List<AgentInfo> agents = loginState.getAgentList();
                agentListCount = agents != null ? agents.size() : 0;
                agentListReceived = phase == LoginState.Phase.AGENTS_RECEIVED || agentListCount > 0;
            }
        } catch (Exception e) {
            logErrorThrottled(ctx, "LOGIN_SNAPSHOT_02", e);
        }
        boolean serverConnected = false;
        try {
            serverConnected = ctx.getDispatcher() != null && ctx.getDispatcher().isServerConnected();
        } catch (Exception ignored) {
        }
        boolean loggedIn = isLoggedIn(ctx);
        return new FlowSnapshot(
                loginState, phase, failureReason, agentListReceived, agentListCount, serverConnected, loggedIn,
                System.currentTimeMillis()
        );
    }

    boolean isLoggedIn(IWorkflowContext ctx) {
        try {
            if (ctx.getGameModel() == null || ctx.getGameModel().getTrainer() == null) {
                return false;
            }
            return ctx.getGameModel().getTrainer().getUniqueId() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    LoginSettingsSnapshot resolveRuntimeSettings(IWorkflowContext ctx, boolean fetchFromProvider) {
        boolean explicitConnect = Boolean.TRUE.equals(ctx.getPersistentData().get("explicitConnectRequested"));
        ILoginSettingsSnapshotter snapshotter = deps.loginSettingsSnapshotter();
        boolean forceRefresh = explicitConnect || fetchFromProvider;
        LoginSettingsSnapshot snapshot = snapshotter.snapshot(ctx, forceRefresh);
        if (explicitConnect) {
            ctx.getPersistentData().remove("uiLoginPhase");
            ctx.getPersistentData().remove(LoginCycleKeys.KEY_UI_LOGIN_PHASE_EVENT_DEDUP);
            ctx.getPersistentData().remove(LoginCycleKeys.KEY_LOGIN_LAST_HALT_SIGNATURE);
            ctx.getPersistentData().remove(LoginCycleKeys.KEY_USER_RESUME_REQUIRED);
            ctx.getPersistentData().remove(LoginCycleKeys.KEY_LOGIN_HALTED_CREDENTIAL);
            ctx.getPersistentData().remove(LoginCycleKeys.KEY_LAST_GATEWAY_LOGIN_REQUEST_MS);
            try {
                LoginState ls = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                if (ls != null && ls.getPhase() == LoginState.Phase.FAILED) {
                    ls.setPhase(LoginState.Phase.DISCONNECTED);
                    ls.setFailureReason(null);
                    ls.clearLoginResultCodes();
                }
            } catch (Exception ignored) {
            }
        }
        return snapshot;
    }

    boolean isHalted(IWorkflowContext ctx, LoginState loginState) {
        if (Boolean.TRUE.equals(ctx.getPersistentData().get(LoginCycleKeys.KEY_LOGIN_HALTED_CREDENTIAL))) {
            return true;
        }
        try {
            if (loginState != null && loginState.getPhase() == LoginState.Phase.FAILED) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    List<HostPort> parseGatewayEndpoints(String raw) {
        if (raw == null || raw.isEmpty()) {
            log.warn("No target gateway configured");
            return Collections.emptyList();
        }
        List<HostPort> endpoints = new ArrayList<>();
        for (String token : String.valueOf(raw).split(",")) {
            HostPort hp = parseGatewayEndpoint(String.valueOf(token));
            if (hp != null) {
                endpoints.add(hp);
            }
        }
        if (endpoints.isEmpty()) {
            log.warn("No valid target gateways configured (raw='{}')", raw);
        }
        return endpoints;
    }

    HostPort parseGatewayEndpoint(String token) {
        String value = String.valueOf(token).trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            if (value.startsWith("[") && value.contains("]")) {
                int close = value.indexOf(']');
                String host = value.substring(1, close).trim();
                String remainder = value.substring(close + 1).trim();
                int port = 15779;
                if (remainder.startsWith(":")) {
                    port = Integer.parseInt(remainder.substring(1).trim());
                } else if (!remainder.isEmpty()) {
                    return null;
                }
                return new HostPort(host, port);
            }
            int firstColon = value.indexOf(':');
            int lastColon = value.lastIndexOf(':');
            if (firstColon >= 0 && firstColon == lastColon) {
                String host = value.substring(0, firstColon).trim();
                int port = Integer.parseInt(value.substring(firstColon + 1).trim());
                return new HostPort(host, port);
            }
            if (firstColon >= 0 && firstColon != lastColon) {
                return new HostPort(value, 15779);
            }
            return new HostPort(value, 15779);
        } catch (NumberFormatException e) {
            log.warn("Invalid target gateway format '{}', expected host:port or [ipv6]:port", value);
            return null;
        }
    }

    String normalizeHostForSocket(String host) {
        String value = String.valueOf(host).trim();
        if (value.startsWith("[") && value.endsWith("]") && value.length() > 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    HostPort resolveGatewayForAttempt(IWorkflowContext ctx, String raw) {
        List<HostPort> endpoints = parseGatewayEndpoints(raw);
        if (endpoints.isEmpty()) {
            return null;
        }
        int index = nextGatewayIndex(ctx, endpoints.size());
        HostPort selected = endpoints.get(index);
        ctx.getPersistentData().put(LoginCycleKeys.KEY_GATEWAY_ENDPOINTS, new ArrayList<>(endpoints));
        ctx.getPersistentData().put(LoginCycleKeys.KEY_GATEWAY_ENDPOINT_INDEX, index);
        return selected;
    }

    @SuppressWarnings("unchecked")
    int nextGatewayIndex(IWorkflowContext ctx, int size) {
        if (size <= 1) {
            return 0;
        }
        long now = System.currentTimeMillis();
        int lastKnownGood = toInt(ctx.getPersistentData().get(LoginCycleKeys.KEY_GATEWAY_KNOWN_GOOD_INDEX), -1);
        long connectedAt = toLong(ctx.getPersistentData().get(LoginCycleKeys.KEY_GATEWAY_CONNECTED_AT_MS), 0L);
        long resolvedAt = toLong(ctx.getPersistentData().get(LoginCycleKeys.KEY_GATEWAY_DNS_RESOLVED_AT_MS), 0L);
        boolean longSession = connectedAt > 0L && (now - connectedAt) > LoginCycleKeys.DNS_STICKY_TTL_MS;
        boolean staleResolution = resolvedAt > 0L && (now - resolvedAt) > LoginCycleKeys.DNS_STICKY_TTL_MS;
        if (lastKnownGood >= 0 && lastKnownGood < size && !(longSession || staleResolution)) {
            return lastKnownGood;
        }
        int current = toInt(ctx.getPersistentData().get(LoginCycleKeys.KEY_GATEWAY_ENDPOINT_INDEX), -1);
        if (current < 0) {
            return (int) (Math.abs(now) % size);
        }
        return (current + 1) % size;
    }

    void markGatewayConnectSuccess(IWorkflowContext ctx) {
        int idx = toInt(ctx.getPersistentData().get(LoginCycleKeys.KEY_GATEWAY_ENDPOINT_INDEX), -1);
        if (idx >= 0) {
            ctx.getPersistentData().put(LoginCycleKeys.KEY_GATEWAY_KNOWN_GOOD_INDEX, idx);
        }
        long now = System.currentTimeMillis();
        ctx.getPersistentData().put(LoginCycleKeys.KEY_GATEWAY_CONNECTED_AT_MS, now);
        ctx.getPersistentData().put(LoginCycleKeys.KEY_GATEWAY_DNS_RESOLVED_AT_MS, now);
    }

    void markGatewayAttemptFailure(IWorkflowContext ctx) {
        ctx.getPersistentData().remove(LoginCycleKeys.KEY_GATEWAY_DNS_RESOLVED_AT_MS);
    }

    boolean hasAgents(LoginState loginState) {
        try {
            return loginState != null && loginState.getAgentList() != null && !loginState.getAgentList().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    boolean hasAgentListReceived(LoginState loginState) {
        try {
            if (hasAgents(loginState)) {
                return true;
            }
            LoginState.Phase phase = loginState != null ? loginState.getPhase() : null;
            return phase == LoginState.Phase.AGENTS_RECEIVED;
        } catch (Exception ignored) {
            return false;
        }
    }

    String evaluatePreflightStatus(LoginSettingsSnapshot settings, LoginState loginState) {
        if (settings == null) {
            return "MISSING_CREDENTIALS";
        }
        if (isBlank(settings.getUsername()) || isBlank(settings.getPassword())) {
            return "MISSING_CREDENTIALS";
        }
        List<AgentInfo> agents = loginState != null ? loginState.getAgentList() : null;
        if (agents == null || agents.isEmpty()) {
            return "SERVER_INSPECTION";
        }
        if (isBlank(settings.getTargetAgent())) {
            return "MISSING_AGENT_SERVER";
        }
        short selectedAgentId = parseAgentId(String.valueOf(settings.getTargetAgent()));
        AgentInfo selected = null;
        for (AgentInfo a : agents) {
            try {
                if (a.getId() == selectedAgentId) {
                    selected = a;
                    break;
                }
            } catch (Exception ignored) {
            }
        }
        if (selected == null) {
            return "MISSING_AGENT_SERVER";
        }
        byte status = 1;
        int online = 0;
        int capacity = 0;
        try {
            status = selected.getStatus();
        } catch (Exception ignored) {
        }
        try {
            online = selected.getOnlineCount() & 0xFFFF;
        } catch (Exception ignored) {
        }
        try {
            capacity = selected.getCapacity() & 0xFFFF;
        } catch (Exception ignored) {
        }
        boolean serverFull = capacity > 0 && online >= capacity;
        if (!serverFull && status == 0) {
            return "SERVER_INSPECTION";
        }
        return "READY";
    }

    boolean loginPrereqsForGateway(LoginSettingsSnapshot settings, LoginState loginState, IWorkflowContext ctx) {
        if (Boolean.TRUE.equals(ctx.getPersistentData().get(LoginCycleKeys.KEY_LOGIN_HALTED_CREDENTIAL))) {
            return false;
        }
        return "READY".equals(evaluatePreflightStatus(settings, loginState));
    }

    static String normalizeGatewayClientModule(Object v) {
        String m = String.valueOf(v != null ? v : "SR_Client").trim();
        return m.isEmpty() ? "SR_Client" : m;
    }

    void applyGatewayHandshakeHintsToProxy(IWorkflowContext ctx, LoginSettingsSnapshot settings) {
        try {
            IProxyConnection proxy = ctx.getProxyConnection();
            if (proxy == null || settings == null) {
                return;
            }
            byte loc = effectiveGatewayLocale(settings);
            String mod = normalizeGatewayClientModule(settings.getGatewayClientModule());
            int ver = toInt(settings.getGatewayClientVersion(), 188);
            proxy.setGatewayHandshakeHints(loc, mod, ver);
        } catch (Exception e) {
            log.debug("setGatewayHandshakeHints skipped: {}", e.getMessage());
        }
    }

    byte effectiveGatewayLocale(LoginSettingsSnapshot settings) {
        if (settings == null) {
            return (byte) 22;
        }
        int v = settings.getLocale();
        if (v == 0) {
            return (byte) 22;
        }
        return (byte) (v & 0xFF);
    }

    long effectiveGatewayLoginMinIntervalMs(LoginSettingsSnapshot settings) {
        if (settings != null) {
            return Math.max(0L, settings.getGatewayLoginMinIntervalMs());
        }
        String prop = System.getProperty("sokybot.login.request.minIntervalMs");
        if (prop != null && !prop.isBlank()) {
            try {
                return Math.max(0L, Long.parseLong(prop.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return 2000L;
    }

    long effectiveGatewayLoginPauseAfterAgentListMs(LoginSettingsSnapshot settings) {
        if (settings != null) {
            return Math.max(0L, Math.min(60000L, settings.getGatewayLoginPauseAfterAgentListMs()));
        }
        String prop = System.getProperty("sokybot.login.pauseAfterAgentListMs");
        if (prop != null && !prop.isBlank()) {
            try {
                return Math.max(0L, Math.min(60000L, Long.parseLong(prop.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        return 1500L;
    }

    boolean allowGatewayLoginRequestNow(IWorkflowContext ctx, LoginSettingsSnapshot settings, long nowMs) {
        long minIv = effectiveGatewayLoginMinIntervalMs(settings);
        if (minIv <= 0L) {
            return true;
        }
        Object lastRaw = ctx.getPersistentData().get(LoginCycleKeys.KEY_LAST_GATEWAY_LOGIN_REQUEST_MS);
        if (!(lastRaw instanceof Number)) {
            return true;
        }
        return nowMs - ((Number) lastRaw).longValue() >= minIv;
    }

    boolean safeIsServerConnected(IWorkflowContext ctx) {
        try {
            return ctx.getDispatcher() != null && ctx.getDispatcher().isServerConnected();
        } catch (Exception ignored) {
            return false;
        }
    }

    boolean isAuthenticated(LoginState loginState) {
        try {
            return loginState != null && loginState.getPhase() == LoginState.Phase.AUTHENTICATED;
        } catch (Exception e) {
            return false;
        }
    }

    boolean requiresManualVerification(LoginState loginState) {
        try {
            LoginState.Phase phase = loginState != null ? loginState.getPhase() : null;
            if (phase == LoginState.Phase.WAIT_FOR_CAPTCHA) {
                return true;
            }
            String reason = String.valueOf(loginState != null ? loginState.getFailureReason() : "").toLowerCase();
            if (reason.isEmpty()) {
                return false;
            }
            return reason.contains("captcha")
                    || reason.contains("image")
                    || reason.contains("verification")
                    || reason.contains("security");
        } catch (Exception e) {
            return false;
        }
    }

    boolean isPasscodeInputRequired(LoginState loginState, IWorkflowContext ctx) {
        try {
            LoginState.Phase phase = loginState != null ? loginState.getPhase() : null;
            if (phase == LoginState.Phase.WAITING_FOR_PASSCODE
                    || phase == LoginState.Phase.WAIT_FOR_CAPTCHA
                    || phase == LoginState.Phase.PASSCODE_SUBMITTED) {
                return true;
            }
            boolean allowStringDetection = true;
            try {
                LoginSettings ls = deps.loginSettingsProvider().get();
                if (ls != null) {
                    allowStringDetection = ls.isPasscodeStringDetectionEnabled();
                }
            } catch (Exception ignored) {
            }
            if (!allowStringDetection) {
                return false;
            }
            String reason = String.valueOf(loginState != null ? loginState.getFailureReason() : "").toLowerCase();
            if (reason.isEmpty()) {
                return false;
            }
            return reason.contains("passcode")
                    || reason.contains("pin")
                    || reason.contains("otp")
                    || reason.contains("captcha")
                    || reason.contains("security code");
        } catch (Exception ignored) {
            return false;
        }
    }

    long readInteractiveWaitUntil(IWorkflowContext ctx) {
        Object existing = ctx.getPersistentData().get(LoginCycleKeys.KEY_INTERACTIVE_WAIT_UNTIL_MS);
        return (existing instanceof Number) ? ((Number) existing).longValue() : 0L;
    }

    boolean isInteractiveWaitTimedOut(IWorkflowContext ctx) {
        long waitUntil = readInteractiveWaitUntil(ctx);
        return waitUntil > 0L && System.currentTimeMillis() >= waitUntil;
    }

    long nextRetryDelayMs(IWorkflowContext ctx, LoginSettingsSnapshot settings) {
        Object attemptRaw = ctx.getPersistentData().getOrDefault("loginRetryAttempt", 0);
        int attempt = (attemptRaw instanceof Number) ? ((Number) attemptRaw).intValue() : 0;
        int nextAttempt = attempt + 1;
        ctx.getPersistentData().put("loginRetryAttempt", nextAttempt);

        if (settings == null) {
            int index = Math.max(0, Math.min(nextAttempt - 1, RETRY_DELAYS_MS.length - 1));
            return clampDelay(RETRY_DELAYS_MS[index]);
        }

        long baseDelay = Math.max(0L, settings.getRetryBaseDelayMs());
        long maxDelay = Math.max(baseDelay, settings.getRetryMaxDelayMs());
        maxDelay = Math.min(LoginCycleKeys.MAX_RETRY_DELAY_MS, maxDelay);

        long cap = baseDelay;
        if (nextAttempt > 1) {
            long exp = (1L << Math.min(nextAttempt - 1, 20));
            cap = baseDelay * exp;
        }
        cap = Math.min(cap, maxDelay);
        if (cap < 0L) {
            cap = maxDelay;
        }

        long jittered = (cap <= 0L) ? 0L : ThreadLocalRandom.current().nextLong(0L, cap + 1L);
        return clampDelay(Math.max(LoginCycleKeys.MIN_RETRY_DELAY_MS, jittered));
    }

    boolean canRetry(IWorkflowContext ctx, LoginSettingsSnapshot settings, String failureClass) {
        if (settings == null) {
            return true;
        }
        boolean infiniteRequested = settings.isInfiniteRetryMode();
        if (infiniteRequested && (LoginCycleKeys.FAILURE_NETWORK.equals(failureClass)
                || LoginCycleKeys.FAILURE_AGENT_TIMEOUT.equals(failureClass)
                || LoginCycleKeys.FAILURE_SERVER_INSPECTION.equals(failureClass)
                || LoginCycleKeys.FAILURE_GHOST_COOLDOWN.equals(failureClass))) {
            return true;
        }
        if (infiniteRequested && !LoginCycleKeys.FAILURE_NETWORK.equals(failureClass)
                && !LoginCycleKeys.FAILURE_AGENT_TIMEOUT.equals(failureClass)
                && !LoginCycleKeys.FAILURE_SERVER_INSPECTION.equals(failureClass)
                && !LoginCycleKeys.FAILURE_GHOST_COOLDOWN.equals(failureClass)) {
            return false;
        }

        int maxAttempts = settings.getMaxRetryAttempts();
        if (maxAttempts <= 0) {
            return true;
        }
        Object attemptRaw = ctx.getPersistentData().getOrDefault("loginRetryAttempt", 0);
        int attempt = (attemptRaw instanceof Number) ? ((Number) attemptRaw).intValue() : 0;
        return attempt < maxAttempts;
    }

    long effectiveAgentWaitTimeoutMs(LoginSettingsSnapshot settings) {
        if (settings != null) {
            return settings.getAgentWaitTimeoutMs();
        }
        return LoginCycleKeys.AGENT_WAIT_TIMEOUT_MS;
    }

    long effectiveLoginResponseTimeoutMs(LoginSettingsSnapshot settings) {
        if (settings != null) {
            return settings.getLoginResponseTimeoutMs();
        }
        return LoginCycleKeys.LOGIN_RESPONSE_TIMEOUT_MS;
    }

    long effectiveAgentAuthTimeoutMs(LoginSettingsSnapshot settings) {
        if (settings != null) {
            return settings.getAgentAuthTimeoutMs();
        }
        return LoginCycleKeys.AGENT_AUTH_TIMEOUT_MS;
    }

    boolean requestAgentList(IWorkflowContext ctx, LoginSettingsSnapshot settings, boolean allowBypass) {
        long now = System.currentTimeMillis();
        long cacheAt = (ctx.getPersistentData().get(LoginCycleKeys.KEY_AGENT_LIST_CACHE_AT_MS) instanceof Number)
                ? ((Number) ctx.getPersistentData().get(LoginCycleKeys.KEY_AGENT_LIST_CACHE_AT_MS)).longValue() : 0L;
        FlowSnapshot s = snapshot(ctx);
        if (cacheAt > 0L && (now - cacheAt) <= 300000L && s.agentListReceived) {
            log.info("Using cached agent list (age={}ms), skipping 0x6101", now - cacheAt);
            return true;
        }
        long banUntil = (ctx.getPersistentData().get("agentBanUntilMs") instanceof Number)
                ? ((Number) ctx.getPersistentData().get("agentBanUntilMs")).longValue() : 0L;
        if (banUntil > now) {
            log.warn("Agent request blocked by ban window for {} ms", banUntil - now);
            return false;
        }
        try {
            IGatewayProtocolEmitter protocol = deps.gatewayProtocolEmitter();
            return protocol.requestAgentList(ctx, allowBypass, LoginCycleKeys.AGENT_REQUEST_MIN_INTERVAL_MS);
        } catch (Exception e) {
            long cooldown = settings != null ? settings.getAgentRequestRetryBackoffMs() : 2000L;
            ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_BYPASS_COOLDOWN_UNTIL_MS, now + cooldown);
            log.warn("Agent request failed: {}", e.getMessage());
            return false;
        }
    }

    void sendLoginRequest(IWorkflowContext ctx, byte locale, String username, String password, short agentId, String charsetName) {
        IGatewayProtocolEmitter protocol = deps.gatewayProtocolEmitter();
        protocol.sendLoginRequest(
                ctx,
                new GatewayCredentials(username, password),
                locale,
                (int) agentId & 0xFFFF,
                charsetName,
                true
        );
    }

    void sendGatewayImageCodeAnswer(IWorkflowContext ctx, String answer) {
        deps.gatewayProtocolEmitter().sendGatewayImageCodeAnswer(ctx, answer);
    }

    short parseAgentId(String targetAgent) {
        if (targetAgent == null || targetAgent.isEmpty()) {
            return (short) 0;
        }
        try {
            return Short.parseShort(targetAgent);
        } catch (NumberFormatException e) {
            log.warn("Invalid agent ID format: {}", targetAgent);
            return (short) 0;
        }
    }

    void beginAttemptSnapshot(IWorkflowContext ctx, LoginSettingsSnapshot settings) {
        if (settings == null) {
            return;
        }
        ctx.getPersistentData().put(LoginCycleKeys.KEY_ATTEMPT_LOGIN_SETTINGS, settings);
        long current = currentAttemptId(ctx);
        ctx.getPersistentData().put(LoginCycleKeys.KEY_ACTIVE_ATTEMPT_ID, current + 1L);
    }

    LoginSettingsSnapshot currentAttemptSettings(IWorkflowContext ctx) {
        Object existing = ctx.getPersistentData().get(LoginCycleKeys.KEY_ATTEMPT_LOGIN_SETTINGS);
        if (existing instanceof LoginSettingsSnapshot) {
            return (LoginSettingsSnapshot) existing;
        }
        LoginSettingsSnapshot settings = resolveRuntimeSettings(ctx, true);
        if (settings != null) {
            beginAttemptSnapshot(ctx, settings);
        }
        return settings;
    }

    long currentAttemptId(IWorkflowContext ctx) {
        Object raw = ctx.getPersistentData().get(LoginCycleKeys.KEY_ACTIVE_ATTEMPT_ID);
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        return 0L;
    }

    void resetRetry(IWorkflowContext ctx) {
        ctx.getPersistentData().put("loginRetryAttempt", 0);
        ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_WAIT_TIMED_OUT, false);
        ctx.getPersistentData().remove(LoginCycleKeys.KEY_RETRY_UNTIL_MS);
        ctx.getPersistentData().remove(LoginCycleKeys.KEY_RETRY_ATTEMPT_ID);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> volatileState(IWorkflowContext ctx) {
        Object existing = ctx.getPersistentData().get(LoginCycleKeys.KEY_VOLATILE_STATE);
        if (existing instanceof ConcurrentHashMap) {
            return (Map<String, Object>) existing;
        }
        ConcurrentHashMap<String, Object> created = new ConcurrentHashMap<>();
        ctx.getPersistentData().put(LoginCycleKeys.KEY_VOLATILE_STATE, created);
        created.putIfAbsent(LoginCycleKeys.KEY_VOLATILE_RESET_TOKEN, 0L);
        return created;
    }

    long bumpResetToken(IWorkflowContext ctx) {
        Map<String, Object> state = volatileState(ctx);
        Object v = state.compute(LoginCycleKeys.KEY_VOLATILE_RESET_TOKEN, (k, cur) -> {
            long c = (cur instanceof Number) ? ((Number) cur).longValue() : 0L;
            return c + 1L;
        });
        return ((Number) v).longValue();
    }

    boolean putVolatileWithToken(IWorkflowContext ctx, String key, Object value, long expectedToken) {
        Map<String, Object> state = volatileState(ctx);
        state.compute(key, (k, existing) -> {
            long current = toLong(state.get(LoginCycleKeys.KEY_VOLATILE_RESET_TOKEN), 0L);
            if (current != expectedToken) {
                return existing;
            }
            return value;
        });
        long current = toLong(state.get(LoginCycleKeys.KEY_VOLATILE_RESET_TOKEN), 0L);
        return current == expectedToken;
    }

    void clearVolatileAttributes(IWorkflowContext ctx) {
        Map<String, Object> state = volatileState(ctx);
        long next = toLong(state.get(LoginCycleKeys.KEY_VOLATILE_RESET_TOKEN), 0L) + 1L;
        state.clear();
        state.put(LoginCycleKeys.KEY_VOLATILE_RESET_TOKEN, next);
    }

    void logInfoCtx(IWorkflowContext ctx, String template, Object... args) {
        if (!log.isInfoEnabled()) {
            return;
        }
        Object[] prefixed = new Object[args.length + 2];
        prefixed[0] = ctx.getMachineName();
        prefixed[1] = Thread.currentThread().getName();
        System.arraycopy(args, 0, prefixed, 2, args.length);
        log.info("[machine={}] [thread={}] " + template, prefixed);
    }

    void logWarnCtx(IWorkflowContext ctx, String template, Object... args) {
        Object[] prefixed = new Object[args.length + 2];
        prefixed[0] = ctx.getMachineName();
        prefixed[1] = Thread.currentThread().getName();
        System.arraycopy(args, 0, prefixed, 2, args.length);
        log.warn("[machine={}] [thread={}] " + template, prefixed);
    }

    void logErrorThrottled(IWorkflowContext ctx, String siteId, Exception e) {
        Map<String, Object> throttle = (Map<String, Object>) ctx.getPersistentData().get(LoginCycleKeys.KEY_ERROR_THROTTLE);
        if (!(throttle instanceof Map)) {
            throttle = new LinkedHashMap<String, Object>(128, 0.75f, true);
            ctx.getPersistentData().put(LoginCycleKeys.KEY_ERROR_THROTTLE, throttle);
        }
        long now = System.currentTimeMillis();
        String key = String.valueOf(ctx.getMachineName()) + "|" + siteId + "|" + e.getClass().getSimpleName();
        Long last = (Long) throttle.get(key);
        if (last == null || (now - last.longValue()) > 60000L) {
            logWarnCtx(ctx, "[{}] {}", siteId, e.getMessage());
            throttle.put(key, now);
        }
        if (throttle.size() > 1000) {
            Iterator<String> it = throttle.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    boolean isAgentWaitTimedOut(IWorkflowContext ctx) {
        try {
            Object deadlineRaw = ctx.getPersistentData().get(LoginCycleKeys.KEY_AGENT_WAIT_DEADLINE_MS);
            if (!(deadlineRaw instanceof Number)) {
                return false;
            }
            long deadlineMs = ((Number) deadlineRaw).longValue();
            return deadlineMs > 0L && System.currentTimeMillis() >= deadlineMs;
        } catch (Exception ignored) {
            return false;
        }
    }

    boolean isLoginResponseTimedOut(IWorkflowContext ctx) {
        try {
            Object deadlineRaw = ctx.getPersistentData().get(LoginCycleKeys.KEY_LOGIN_RESPONSE_DEADLINE_MS);
            if (!(deadlineRaw instanceof Number)) {
                return false;
            }
            long deadlineMs = ((Number) deadlineRaw).longValue();
            return deadlineMs > 0L && System.currentTimeMillis() >= deadlineMs;
        } catch (Exception ignored) {
            return false;
        }
    }

    boolean isAgentAuthTimedOut(IWorkflowContext ctx) {
        try {
            Object deadlineRaw = ctx.getPersistentData().get(LoginCycleKeys.KEY_AGENT_AUTH_DEADLINE_MS);
            if (!(deadlineRaw instanceof Number)) {
                return false;
            }
            long deadlineMs = ((Number) deadlineRaw).longValue();
            return deadlineMs > 0L && System.currentTimeMillis() >= deadlineMs;
        } catch (Exception ignored) {
            return false;
        }
    }

    boolean isBlank(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> readLatestNetworkFailure(IWorkflowContext ctx) {
        try {
            Object raw = ctx.getPersistentData().get(LoginCycleKeys.KEY_NETWORK_TRANSITIONS);
            if (!(raw instanceof List)) {
                return null;
            }
            List<Object> list = (List<Object>) raw;
            for (int i = list.size() - 1; i >= 0; i--) {
                Object ev = list.get(i);
                if (!(ev instanceof Map)) {
                    continue;
                }
                Map<String, Object> m = (Map<String, Object>) ev;
                String transition = String.valueOf(m.getOrDefault("transition", ""));
                if ("Disconnected".equalsIgnoreCase(transition) || "AUTH_FAILED".equalsIgnoreCase(String.valueOf(m.get("loginPhase")))) {
                    return m;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    LoginFailureVerdict classifyFailureVerdict(LoginState loginState, IWorkflowContext ctx) {
        return deps.loginFailureClassifier().classify(ctx, String.valueOf(loginState != null ? loginState.getFailureReason() : ""));
    }

    String classifyFailure(LoginState loginState, IWorkflowContext ctx) {
        LoginFailureVerdict verdict = classifyFailureVerdict(loginState, ctx);
        if (verdict == null || verdict.getFailureClass() == null) {
            return LoginCycleKeys.FAILURE_UNKNOWN_RETRY;
        }
        return String.valueOf(verdict.getFailureClass().name());
    }

    boolean handleInteractivePasscodeWait(IWorkflowContext ctx, LoginState loginState, FlowSnapshot snap, LoginSettingsSnapshot settings) {
        InteractiveOutcome outcome = deps.loginInteractiveCoordinator().handle(ctx, settings);
        if (outcome == InteractiveOutcome.WAITING_FOR_PASSCODE) {
            emitEnginePhase(ctx, LoginCycleKeys.PHASE_WAITING_FOR_PASSCODE, "WaitingForPasscode", String.valueOf(snap.failureReason != null ? snap.failureReason : ""));
            return true;
        }
        if (outcome == InteractiveOutcome.WAITING_FOR_CAPTCHA) {
            emitEnginePhase(ctx, LoginCycleKeys.PHASE_WAIT_FOR_CAPTCHA, "WaitingForPasscode", String.valueOf(snap.failureReason != null ? snap.failureReason : ""));
            return true;
        }
        return false;
    }

    boolean handleQueueWait(IWorkflowContext ctx, LoginState loginState) {
        InteractiveOutcome outcome = deps.loginInteractiveCoordinator().handle(ctx, resolveRuntimeSettings(ctx, false));
        if (outcome == InteractiveOutcome.IN_QUEUE) {
            emitEnginePhase(ctx, LoginCycleKeys.PHASE_IN_QUEUE, "QueueWaiting", String.valueOf(loginState != null ? loginState.getFailureReason() : ""),
                    Collections.singletonMap("queuePosition", parseQueuePosition(loginState)));
            return true;
        }
        return false;
    }

    boolean handleMissingPrereq(IWorkflowContext ctx) {
        InteractiveOutcome outcome = deps.loginInteractiveCoordinator().handle(ctx, resolveRuntimeSettings(ctx, false));
        if (outcome == InteractiveOutcome.WAITING_FOR_USER_RESUME) {
            ctx.getPersistentData().remove(LoginCycleKeys.KEY_RETRY_UNTIL_MS);
            ctx.getPersistentData().remove(LoginCycleKeys.KEY_RETRY_ATTEMPT_ID);
            return true;
        }
        return false;
    }

    boolean handleTerminalFailure(IWorkflowContext ctx, LoginState loginState, LoginFailureVerdict verdict, LoginSettingsSnapshot settings) {
        String failureClass = verdict != null && verdict.getFailureClass() != null
                ? String.valueOf(verdict.getFailureClass().name())
                : LoginCycleKeys.FAILURE_UNKNOWN_RETRY;
        if (LoginFailureClass.MISSING_PREREQ.name().equals(failureClass)) {
            ctx.getPersistentData().put(LoginCycleKeys.KEY_USER_RESUME_REQUIRED, true);
            return true;
        }
        String haltSignature = buildHaltSignature(ctx, loginState, failureClass);
        String lastHaltSignature = String.valueOf(ctx.getPersistentData().getOrDefault(LoginCycleKeys.KEY_LOGIN_LAST_HALT_SIGNATURE, ""));
        boolean haltAlreadyLogged = haltSignature.equals(lastHaltSignature);

        if (LoginFailureClass.MANUAL_VERIFICATION.name().equals(failureClass)) {
            Map<String, Object> extras = new HashMap<>();
            extras.put("failureClass", failureClass);
            extras.put("fatal", true);
            emitEnginePhase(ctx, "MANUAL_VERIFICATION_REQUIRED", "ManualVerification", String.valueOf(loginState != null ? loginState.getFailureReason() : ""), extras);
            if (!haltAlreadyLogged) {
                log.warn("Login paused: manual verification required ({})", loginState != null ? loginState.getFailureReason() : "");
                ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature);
            }
            return true;
        }
        if (LoginFailureClass.CREDENTIAL.name().equals(failureClass)) {
            ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_HALTED_CREDENTIAL, true);
            String credDetail = credentialFailureDetail(loginState);
            Map<String, Object> extras = new HashMap<>();
            extras.put("failureClass", failureClass);
            extras.put("fatal", true);
            emitEnginePhase(ctx, LoginState.Phase.FAILED.name(), "CredentialFailure", credDetail, extras);
            if (!haltAlreadyLogged) {
                log.warn("Infinite retry disabled for credential failures");
                log.error("Login halted: credential failure ({})", credDetail.isEmpty() ? "unknown" : credDetail);
                ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature);
            }
            return true;
        }
        if (LoginFailureClass.CHARACTER_NOT_FOUND.name().equals(failureClass)) {
            Map<String, Object> extras = new HashMap<>();
            extras.put("failureClass", failureClass);
            extras.put("fatal", true);
            emitEnginePhase(ctx, LoginState.Phase.FAILED.name(), "CharacterUnavailable",
                    String.valueOf(loginState != null ? loginState.getFailureReason() : "CHARACTER_SELECTION_UNAVAILABLE"), extras);
            return true;
        }
        if (LoginFailureClass.AGENT_TIMEOUT.name().equals(failureClass)) {
            emitEnginePhase(ctx, LoginCycleKeys.PHASE_WAITING_FOR_AGENTS_TIMEOUT, "WaitingForAgentsTimeout",
                    String.valueOf(loginState != null ? loginState.getFailureReason() : "Agent list timeout"));
        }
        if (LoginFailureClass.SERVER_INSPECTION.name().equals(failureClass)) {
            long now = System.currentTimeMillis();
            long delayMs = Math.max(nextRetryDelayMs(ctx, settings), verdict != null ? verdict.getRetryDelayFloorMs() : 60000L);
            Map<String, Object> extras = new HashMap<>();
            extras.put("retryDelayMs", delayMs);
            extras.put("serverTimestamp", now);
            extras.put("retryAt", now + delayMs);
            extras.put("failureClass", failureClass);
            emitEnginePhase(ctx, LoginCycleKeys.PHASE_SERVER_INSPECTION, "ServerInspection",
                    String.valueOf(loginState != null ? loginState.getFailureReason() : "Target server is under inspection."), extras);
            ctx.getPersistentData().put(LoginCycleKeys.KEY_RETRY_UNTIL_MS, now + delayMs);
            ctx.getPersistentData().put(LoginCycleKeys.KEY_RETRY_ATTEMPT_ID, currentAttemptId(ctx));
            return true;
        }
        if (settings != null && !settings.isAutoReconnect()) {
            Map<String, Object> extras = new HashMap<>();
            extras.put("failureClass", failureClass);
            extras.put("fatal", true);
            emitEnginePhase(ctx, "RETRY_DISABLED", "RetryDisabled", null, extras);
            if (!haltAlreadyLogged) {
                log.warn("Login retries halted: auto reconnect disabled in settings");
                ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature);
            }
            return true;
        }

        if (settings != null && !canRetry(ctx, settings, failureClass)) {
            Map<String, Object> extras = new HashMap<>();
            extras.put("failureClass", failureClass);
            extras.put("fatal", true);
            emitEnginePhase(ctx, "RETRY_LIMIT_REACHED", "RetryLimitReached", null, extras);
            if (!haltAlreadyLogged) {
                log.warn("Login retries halted: max retry attempts reached ({})", settings.getMaxRetryAttempts());
                ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature);
            }
            return true;
        }
        ctx.getPersistentData().remove(LoginCycleKeys.KEY_LOGIN_LAST_HALT_SIGNATURE);
        return false;
    }

    void scheduleRetry(IWorkflowContext ctx, LoginState loginState, FlowSnapshot snap, String failureClass, LoginSettingsSnapshot settings) {
        markGatewayAttemptFailure(ctx);
        long requestedDelayMs = settings != null ? Math.max(0L, settings.getRetryBaseDelayMs()) : RETRY_DELAYS_MS[0];
        long delayMs = nextRetryDelayMs(ctx, settings);
        if (LoginFailureClass.GHOST_COOLDOWN.name().equals(failureClass)) {
            delayMs = Math.max(delayMs, 60000L);
        }
        boolean infiniteRequested = settings != null && settings.isInfiniteRetryMode();
        boolean infiniteEffective = (LoginFailureClass.NETWORK.name().equals(failureClass) || LoginFailureClass.AGENT_TIMEOUT.name().equals(failureClass)) && infiniteRequested;
        String phase = snap.phase != null ? String.valueOf(snap.phase) : "DISCONNECTED";
        String host = String.valueOf(ctx.getPersistentData().get("gatewayHost"));
        String port = String.valueOf(ctx.getPersistentData().get("gatewayPort"));
        log.info("Retry policy applied: requestedDelay={} effectiveDelay={} infinite={}", requestedDelayMs, delayMs, infiniteEffective);
        log.info("RETRY_DELAY context machine={} phase={} serverConnected={} gateway={}:{}",
                ctx.getMachineName(), phase, ctx.getDispatcher().isServerConnected(), host, port);
        log.warn("Login retry in {} ms (reason: {})", delayMs, loginState != null ? loginState.getFailureReason() : "unknown");
        long nowMs = System.currentTimeMillis();
        Map<String, Object> extras = new HashMap<>();
        extras.put("retryDelayMs", delayMs);
        extras.put("serverTimestamp", nowMs);
        extras.put("retryAt", nowMs + delayMs);
        extras.put("failureClass", failureClass);
        emitEnginePhase(ctx, "RETRY_DELAY", "RetryDelay", String.valueOf(loginState != null ? loginState.getFailureReason() : ""), extras);
        ctx.getPersistentData().put(LoginCycleKeys.KEY_RETRY_UNTIL_MS, nowMs + delayMs);
        ctx.getPersistentData().put(LoginCycleKeys.KEY_RETRY_ATTEMPT_ID, currentAttemptId(ctx));

        // Publish SessionReconnectingEvent for the session projection
        int attempt = 1;
        try {
            Object retryRaw = ctx.getPersistentData().getOrDefault("loginRetryAttempt", 0);
            attempt = Math.max(1, (retryRaw instanceof Number) ? ((Number) retryRaw).intValue() : 1);
        } catch (Exception ignored) { }
        publishSessionEvent(ctx, new SessionReconnectingEvent(machineFullName(ctx), attempt, nowMs + delayMs));
    }

    String buildHaltSignature(IWorkflowContext ctx, LoginState loginState, String failureClass) {
        String machine = String.valueOf(ctx.getMachineName());
        String phase = String.valueOf(loginState != null ? loginState.getPhase() : "DISCONNECTED");
        String reason = String.valueOf(loginState != null ? loginState.getFailureReason() : "");
        String connected = "false";
        try {
            connected = String.valueOf(ctx.getDispatcher() != null && ctx.getDispatcher().isServerConnected());
        } catch (Exception ignored) {
        }
        return machine + "|" + phase + "|" + failureClass + "|" + connected + "|" + reason;
    }

    static String credentialFailureDetail(LoginState loginState) {
        return String.valueOf(loginState != null ? loginState.getFailureReason() : "");
    }

    int parseQueuePosition(LoginState loginState) {
        try {
            if (loginState != null && loginState.getQueuePosition() != null) {
                return loginState.getQueuePosition();
            }
            String reason = String.valueOf(loginState != null ? loginState.getFailureReason() : "");
            Matcher m = QUEUE_POSITION.matcher(reason);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    void emitEnginePhase(IWorkflowContext ctx, String phase, String transition, String reason) {
        emitEnginePhase(ctx, phase, transition, reason, null);
    }

    void emitEnginePhase(IWorkflowContext ctx, String phase, String transition, String reason, Map<String, Object> extras) {
        try {
            LoginState state = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
            if (state != null && reason != null && !reason.isEmpty()) {
                state.setFailureReason(reason);
            }
            LoginState.Phase desired = resolveLoginPhaseEnum(phase);
            if (desired == null || state == null || state.getPhase() != desired) {
                setLoginStatePhase(state, phase);
            }
            boolean legacyEmit = Boolean.parseBoolean(System.getProperty("sokybot.engine.phase.legacy", "true"));
            if (!legacyEmit) {
                return;
            }
            EventAdmin eventAdmin = ctx.getServiceOptional(EventAdmin.class).orElse(null);
            if (eventAdmin == null) {
                return;
            }
            String machineId = ctx.getMachineId();
            if (machineId == null || machineId.trim().isEmpty()) {
                machineId = String.valueOf(ctx.getMachineName());
            }
            if (machineId == null || machineId.trim().isEmpty()) {
                return;
            }

            String eventDedupKey = String.valueOf(phase) + '\u0001' + String.valueOf(transition) + '\u0001' + String.valueOf(reason);
            String previousDedup = String.valueOf(ctx.getPersistentData().getOrDefault(LoginCycleKeys.KEY_UI_LOGIN_PHASE_EVENT_DEDUP, ""));
            if (eventDedupKey.equals(previousDedup)) {
                return;
            }
            ctx.getPersistentData().put(LoginCycleKeys.KEY_UI_LOGIN_PHASE_EVENT_DEDUP, eventDedupKey);
            ctx.getPersistentData().put("uiLoginPhase", phase);

            Map<String, Object> props = new HashMap<>();
            props.put("machineId", machineId);
            props.put("transition", transition != null ? transition : "EnginePhase");
            props.put("connected", Boolean.valueOf(ctx.getDispatcher() != null && ctx.getDispatcher().isServerConnected()));
            props.put("authenticated", Boolean.valueOf(isAuthenticated(state)));
            props.put("loginPhase", phase);
            props.put("timestamp", System.currentTimeMillis());
            if (reason != null && !reason.isEmpty()) {
                props.put("reason", reason);
            }
            int retryCount = 0;
            try {
                Object retryRaw = ctx.getPersistentData().getOrDefault("loginRetryAttempt", 0);
                retryCount = (retryRaw instanceof Number) ? ((Number) retryRaw).intValue() : toInt(retryRaw, 0);
            } catch (Exception ignored) {
            }
            props.put("retryCount", Math.max(0, retryCount));

            int maxRetries = 0;
            int configuredClientVersion = 188;
            try {
                LoginSettingsSnapshot runtime = resolveRuntimeSettings(ctx, false);
                if (runtime != null) {
                    maxRetries = Math.max(0, runtime.getMaxRetryAttempts());
                    configuredClientVersion = runtime.getGatewayClientVersion();
                }
            } catch (Exception ignored) {
            }
            props.put("maxRetries", maxRetries);
            props.put("configuredClientVersion", configuredClientVersion);
            String lastFailureReason;
            try {
                lastFailureReason = String.valueOf(
                        (reason != null && !reason.isEmpty()) ? reason : (state != null ? state.getFailureReason() : ""));
            } catch (Exception ignored) {
                lastFailureReason = "";
            }
            if (lastFailureReason != null && !lastFailureReason.isEmpty()) {
                props.put("lastFailureReason", lastFailureReason);
            }
            if (extras != null) {
                props.putAll(extras);
            }
            eventAdmin.postEvent(new Event("sokybot/network/" + osgiEventTopicSeg(machineId) + "/EnginePhase", props));
        } catch (Exception ignored) {
        }
    }

    static String osgiEventTopicSeg(String s) {
        if (s == null || s.isEmpty()) {
            return "_";
        }
        return s.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    static LoginState.Phase resolveLoginPhaseEnum(String phase) {
        if (phase == null) {
            return null;
        }
        return LoginPhaseAliases.resolve(phase).orElse(null);
    }

    static void setLoginStatePhase(LoginState loginState, String phase) {
        if (loginState == null || phase == null) {
            return;
        }
        try {
            LoginPhaseAliases.resolve(phase).ifPresent(loginState::setPhase);
        } catch (Exception ignored) {
        }
    }

    static boolean toBool(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return String.valueOf(value).equalsIgnoreCase("true");
    }

    static int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    static long toLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    static long clampDelay(long value) {
        return Math.max(LoginCycleKeys.MIN_RETRY_DELAY_MS, Math.min(value, LoginCycleKeys.MAX_RETRY_DELAY_MS));
    }

    static String redactForLog(String username) {
        if (username == null || username.isEmpty()) {
            return "<empty>";
        }
        if (username.length() == 1) {
            return "*";
        }
        if (username.length() == 2) {
            return username.charAt(0) + "*";
        }
        return username.charAt(0) + "***" + username.substring(username.length() - 1);
    }

    /**
     * Publish a session lifecycle event via the reactive event bus (obtained from the workflow context).
     * Fails silently if the bus is unavailable.
     */
    void publishSessionEvent(IWorkflowContext ctx, Object event) {
        try {
            IReactiveEventBus bus = ctx.getServiceOptional(IReactiveEventBus.class).orElse(null);
            if (bus != null) {
                bus.publish(event);
            }
        } catch (Exception e) {
            log.debug("Failed to publish session event: {}", e.getMessage());
        }
    }

    /**
     * Build the machine full name from the workflow context (group.machine).
     */
    String machineFullName(IWorkflowContext ctx) {
        String group = ctx.getGroupName();
        String machine = ctx.getMachineName();
        if (group != null && !group.isEmpty() && machine != null && !machine.isEmpty()) {
            return group + "." + machine;
        }
        String machineId = ctx.getMachineId();
        return machineId != null ? machineId : String.valueOf(machine);
    }

}
