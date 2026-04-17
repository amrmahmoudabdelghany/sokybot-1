import org.sokybot.settings.security.Encrypted
import org.sokybot.gamemodel.LoginState
import org.osgi.service.event.Event
import org.osgi.service.event.EventAdmin
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import org.sokybot.engine.api.RateLimitException
import org.sokybot.network.NetworkPeer
import org.sokybot.network.packet.ClientOpcode
import org.sokybot.network.packet.Encoding
import org.sokybot.network.packet.MutablePacket
import groovy.lang.MissingMethodException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class Login extends BaseActuator {
    private def loginSettingsProvider
    private static final List<Long> RETRY_DELAYS_MS = [5000L, 10000L, 30000L, 60000L]
    private static final long MIN_RETRY_DELAY_MS = 5000L
    private static final long MAX_RETRY_DELAY_MS = 300000L
    // Gateway result codes (0xA102 failure byte) observed on common SRO builds.
    private static final int GATEWAY_ERR_INVALID_CREDENTIAL_1 = 1
    private static final int GATEWAY_ERR_INVALID_CREDENTIAL_2 = 2
    private static final int GATEWAY_ERR_ALREADY_CONNECTED = 4
    private static final int GATEWAY_ERR_DISCONNECTED_OR_BLOCKED = 6
    private static final int GATEWAY_ERR_INVALID_CREDENTIAL_0B = 0x0B
    private static final int GATEWAY_ERR_INVALID_CREDENTIAL_0C = 0x0C
    private static final int GATEWAY_ERR_INVALID_CREDENTIAL_0D = 0x0D

    private static final String KEY_LOGIN_HALTED_CREDENTIAL = "loginHaltedCredentialFailure"
    private static final String KEY_LOGIN_LAST_HALT_SIGNATURE = "loginLastHaltSignature"
    private static final String FAILURE_NETWORK = "NETWORK"
    private static final String FAILURE_AGENT_TIMEOUT = "AGENT_TIMEOUT"
    private static final String FAILURE_CREDENTIAL = "CREDENTIAL"
    private static final String FAILURE_GHOST_COOLDOWN = "GHOST_COOLDOWN"
    private static final String FAILURE_MANUAL_VERIFICATION = "MANUAL_VERIFICATION"
    private static final String FAILURE_CHARACTER_NOT_FOUND = "CHARACTER_NOT_FOUND"
    private static final String FAILURE_SERVER_INSPECTION = "SERVER_INSPECTION"
    private static final String FAILURE_MISSING_PREREQ = "MISSING_PREREQ"
    private static final String FAILURE_UNKNOWN_RETRY = "UNKNOWN_RETRY"
    private static final String FAILURE_FATAL = "FATAL"

    // State names (avoid typos in transitions).
    private static final String STATE_CHECK_CONNECTION = "CHECK_CONNECTION"
    private static final String STATE_POST_CHECK_CONNECTION = "POST_CHECK_CONNECTION"
    private static final String STATE_CONNECT_TO_GATEWAY = "CONNECT_TO_GATEWAY"
    private static final String STATE_WAIT_FOR_CONNECTION = "WAIT_FOR_CONNECTION"
    private static final String STATE_RESEND_AGENT_LIST_REQUEST = "RESEND_AGENT_LIST_REQUEST"
    private static final String STATE_WAIT_FOR_AGENTS = "WAIT_FOR_AGENTS"
    private static final String STATE_WAIT_FOR_AGENTS_RECHECK = "WAIT_FOR_AGENTS_RECHECK"
    private static final String STATE_CHECK_AGENT_WAIT_TIMEOUT = "CHECK_AGENT_WAIT_TIMEOUT"
    private static final String STATE_CONNECTED_RESUME = "CONNECTED_RESUME"
    /** Marks intent to send 0x6102; wire delay is enforced in DispatcherImpl from LoginState agent-list wall clock. */
    private static final String STATE_PAUSE_BEFORE_GATEWAY_LOGIN = "PAUSE_BEFORE_GATEWAY_LOGIN"
    private static final String STATE_PARK_MISSING_LOGIN = "PARK_MISSING_LOGIN"
    private static final String STATE_SEND_LOGIN_REQUEST = "SEND_LOGIN_REQUEST"
    private static final String STATE_WAIT_FOR_LOGIN_RESPONSE = "WAIT_FOR_LOGIN_RESPONSE"
    private static final String STATE_WAIT_FOR_LOGIN_RECHECK = "WAIT_FOR_LOGIN_RECHECK"
    private static final String STATE_SUBMIT_GATEWAY_IMAGE_CODE = "SUBMIT_GATEWAY_IMAGE_CODE"
    private static final String STATE_CHECK_LOGIN_FAILED_IMMEDIATE = "CHECK_LOGIN_FAILED_IMMEDIATE"
    private static final String STATE_CHECK_LOGIN_TIMEOUT = "CHECK_LOGIN_TIMEOUT"
    private static final String STATE_CHECK_AGENT_SERVER_CONNECTION_FAILURE = "CHECK_AGENT_SERVER_CONNECTION_FAILURE"
    private static final String STATE_WAIT_FOR_AGENT_SERVER_CONNECTION_RECHECK = "WAIT_FOR_AGENT_SERVER_CONNECTION_RECHECK"
    private static final String STATE_WAIT_FOR_AGENT_AUTH = "WAIT_FOR_AGENT_AUTH"
    private static final String STATE_WAIT_FOR_AGENT_AUTH_RECHECK = "WAIT_FOR_AGENT_AUTH_RECHECK"
    private static final String STATE_CHECK_AGENT_AUTH_TIMEOUT = "CHECK_AGENT_AUTH_TIMEOUT"
    private static final String STATE_WAIT_FOR_CHARACTER = "WAIT_FOR_CHARACTER"
    private static final String STATE_WAIT_FOR_CHARACTER_RECHECK = "WAIT_FOR_CHARACTER_RECHECK"
    private static final String STATE_CHECK_CHARACTER_WAIT_TIMEOUT = "CHECK_CHARACTER_WAIT_TIMEOUT"
    private static final String STATE_RETRY_DELAY = "RETRY_DELAY"
    private static final String STATE_WAIT_RETRY_DELAY = "WAIT_RETRY_DELAY"
    private static final String STATE_WAIT_RETRY_DELAY_RECHECK = "WAIT_RETRY_DELAY_RECHECK"
    // Added in later steps; constant reserved up-front.
    private static final String STATE_WAIT_FOR_AGENT_SERVER_CONNECTION = "WAIT_FOR_AGENT_SERVER_CONNECTION"
    private static final String STATE_CHECK_CONNECTION_ROUTE = "CHECK_CONNECTION_ROUTE"
    private static final String STATE_PARK_MISSING_PREREQS = "PARK_MISSING_PREREQS"
    private static final String STATE_PARK_MANUAL_CONNECT = "PARK_MANUAL_CONNECT"
    private static final String KEY_RUNTIME_LOGIN_SETTINGS = "runtimeLoginSettings"
    private static final String KEY_UI_LOGIN_PHASE_EVENT_DEDUP = "uiLoginPhaseEventDedup"
    private static final String PHASE_MISSING_GATEWAY = "MISSING_GATEWAY"
    private static final String PHASE_PENDING_MANUAL_CONNECT = "PENDING_MANUAL_CONNECT"
    private static final String PHASE_MISSING_CREDENTIALS = "MISSING_CREDENTIALS"
    private static final String PHASE_MISSING_AGENT_SERVER = "MISSING_AGENT_SERVER"
    private static final String PHASE_MISSING_CHARACTER_SELECTION = "MISSING_CHARACTER_SELECTION"
    private static final String PHASE_WAITING_FOR_AGENTS = "WAITING_FOR_AGENTS"
    private static final String PHASE_GATEWAY_LOGIN_PAUSE = "GatewayLoginPause"
    private static final String PHASE_WAITING_FOR_AGENTS_TIMEOUT = "WAITING_FOR_AGENTS_TIMEOUT"
    private static final String PHASE_WAITING_FOR_PASSCODE = "WAITING_FOR_PASSCODE"
    private static final String PHASE_WAIT_FOR_CAPTCHA = "WAIT_FOR_CAPTCHA"
    private static final String PHASE_PASSCODE_SUBMITTED = "PASSCODE_SUBMITTED"
    private static final String PHASE_IN_QUEUE = "IN_QUEUE"
    private static final String PHASE_LOADING_ENVIRONMENT = "LOADING_ENVIRONMENT"
    private static final String PHASE_SERVER_INSPECTION = "SERVER_INSPECTION"
    private static final String KEY_AGENT_WAIT_DEADLINE_MS = "loginAgentWaitDeadlineMs"
    private static final String KEY_AGENT_WAIT_TIMED_OUT = "loginAgentWaitTimedOut"
    private static final String KEY_LOGIN_RESPONSE_DEADLINE_MS = "loginResponseDeadlineMs"
    private static final String KEY_AGENT_AUTH_DEADLINE_MS = "agentAuthDeadlineMs"
    private static final String KEY_RETRY_UNTIL_MS = "loginRetryUntilMs"
    private static final String KEY_ATTEMPT_LOGIN_SETTINGS = "loginAttemptSettings"
    private static final String KEY_ACTIVE_ATTEMPT_ID = "loginActiveAttemptId"
    private static final String KEY_RETRY_ATTEMPT_ID = "loginRetryAttemptId"
    private static final String KEY_INTERACTIVE_WAIT_UNTIL_MS = "loginInteractiveWaitUntilMs"
    private static final String KEY_USER_RESUME_REQUIRED = "loginUserResumeRequired"
    private static final String KEY_LOGIN_IN_PROGRESS = "loginInProgress"
    private static final String KEY_AGENT_BYPASS_COOLDOWN_UNTIL_MS = "agentBypassCooldownUntilMs"
    private static final String KEY_AGENT_LIST_CACHE_AT_MS = "agentListCacheAtMs"
    /** Last successful outbound 0x6102 (per machine); debounces login storms. */
    private static final String KEY_LAST_GATEWAY_LOGIN_REQUEST_MS = "loginLastGatewayLoginRequestMs"
    private static final long AGENT_WAIT_TIMEOUT_MS = 15000L
    private static final long LOGIN_RESPONSE_TIMEOUT_MS = 15000L
    private static final long AGENT_AUTH_TIMEOUT_MS = 45000L
    private static final long PASSCODE_WAIT_TIMEOUT_MS = 60000L
    private static final long PASSCODE_USER_INPUT_TIMEOUT_MS = 60000L
    private static final long LOGOUT_ACK_TIMEOUT_MS = 3000L
    private static final String KEY_NETWORK_TRANSITIONS = "networkTransitions"
    private static final String KEY_GATEWAY_ENDPOINTS = "loginGatewayEndpoints"
    private static final String KEY_GATEWAY_ENDPOINT_INDEX = "loginGatewayEndpointIndex"
    private static final String KEY_GATEWAY_KNOWN_GOOD_INDEX = "loginGatewayKnownGoodIndex"
    private static final String KEY_GATEWAY_CONNECTED_AT_MS = "loginGatewayConnectedAtMs"
    private static final String KEY_GATEWAY_DNS_RESOLVED_AT_MS = "loginGatewayDnsResolvedAtMs"
    private static final String KEY_VOLATILE_STATE = "loginVolatileState"
    private static final String KEY_VOLATILE_RESET_TOKEN = "resetToken"
    private static final String KEY_ERROR_THROTTLE = "loginErrorThrottle"
    private static final long DNS_STICKY_TTL_MS = 60000L

    /** Matches DispatcherImpl agent-request min interval; used only for Groovy 0x6101 fallback. */
    private static final long AGENT_REQUEST_MIN_INTERVAL_MS = 10_000L
    /** Workflow persistent keys: per machine (IWorkflowContext), not script statics. */
    private static final String KEY_FALLBACK_AGENT_REQUEST_LAST_MS = "loginFallbackAgentRequestLastMs"
    private static final String KEY_FALLBACK_AGENT_COLD_BYPASS_USED = "loginFallbackAgentColdBypassUsed"

    /**
     * Resilience for OSGi/API skew: null = unknown, true = use {@code disp.send*} dynamic calls,
     * false = use script packet fallbacks until JVM restart or script reload (new script class).
     * Ops: refresh {@code sokybot-engine} and {@code sokybot-engine-api} together; script fallback masks stale bundles.
     */
    private static final AtomicReference<Boolean> DISPATCHER_DYNAMIC_PACKET_API_OK = new AtomicReference<>(null)
    private static final AtomicBoolean LOGGED_DISPATCHER_PACKET_API_FAILURE = new AtomicBoolean(false)

    /** OSGi Event topics reject {@code '.'} in segments (machine ids are {@code Group.Machine}). */
    private static String osgiEventTopicSeg(String s) {
        if (s == null || s.isEmpty()) return '_'
        return s.replaceAll(/[^A-Za-z0-9_-]/, '_')
    }

    Login() { super("login") }

    @Override
    void setup() {
        // Redeploy/restart sokybot-engine and reload this script together; otherwise 0x6102 may use an older DispatcherImpl (locale 0 on wire).
        registerSettings("login", LoginSettings, { new LoginSettings() })

        def settingsProvider = settingsProvider("login", LoginSettings)
        this.loginSettingsProvider = settingsProvider
        if (settingsProvider == null) {
            log.warn("Login: settings provider not available; cycle will not be registered")
            return
        }

        try {
            settingsProvider.subscribe({ __ ->
                try {
                    if (context?.getPersistentData() != null) {
                        context.getPersistentData().remove(KEY_RUNTIME_LOGIN_SETTINGS)
                    }
                } catch (Exception ignored) {
                }
            })
        } catch (Exception e) {
            log.warn("Login: could not subscribe to login settings changes: {}", e.getMessage())
        }

        def cycle = new CycleDefinitionBuilder()
                .name("login-cycle")
                .priority(200)
                .entryState(STATE_CHECK_CONNECTION)
                .entryGuard({ ctx ->
                    boolean explicitConnect = Boolean.TRUE.equals(ctx.getPersistentData().get("explicitConnectRequested"))
                    if (explicitConnect) {
                        resolveRuntimeSettings(ctx, settingsProvider, true)
                    }
                    def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                    def loginState = ctx.getGameModel()?.getLoginState()

                    log.info("Login-cycle entryGuard: machine={}, explicitConnect={}, hasSettings={}, targetGateway='{}'",
                        ctx.getMachineName(), explicitConnect, settings != null, settings?.targetGateway)

                    if (isHalted(ctx, loginState) && !explicitConnect) {
                        log.info("Login-cycle entryGuard: halted until explicit connect machine={}", ctx.getMachineName())
                        return false
                    }
                    return true
                })
                .entryGuard({ ctx ->
                    boolean explicitConnect = Boolean.TRUE.equals(ctx.getPersistentData().get("explicitConnectRequested"))
                    if (explicitConnect) {
                        resolveRuntimeSettings(ctx, settingsProvider, true)
                    }
                    def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                    boolean haltedByCredential = Boolean.TRUE.equals(ctx.getPersistentData().get(KEY_LOGIN_HALTED_CREDENTIAL))
                    boolean waitingForExplicitResume = Boolean.TRUE.equals(ctx.getPersistentData().get(KEY_USER_RESUME_REQUIRED))
                    if (waitingForExplicitResume && !explicitConnect) {
                        return false
                    }
                    def loginState = ctx.getGameModel()?.getLoginState()
                    if (isPasscodeInputRequired(loginState) && !isInteractiveWaitTimedOut(ctx)) {
                        emitEnginePhase(ctx, PHASE_WAITING_FOR_PASSCODE, "WaitingForPasscode",
                                String.valueOf(loginState?.getFailureReason() ?: ""),
                                [interactive: true, waitUntil: readInteractiveWaitUntil(ctx)])
                        return false
                    }
                    if (haltedByCredential && !explicitConnect) {
                        return false
                    }
                    boolean result = settings != null &&
                            !requiresManualVerification(loginState) &&
                            !isAuthenticated(loginState) &&
                            !isLoggedIn(ctx)
                    if (!result) {
                        logInfoCtx(ctx, "Login entry guard FAILED: settings={} autoLogin={} explicitConnect={} requiresVerification={} isAuthenticated={} isLoggedIn={} haltedByCredential={}",
                                settings != null, toBool(settings?.autoLogin, false), explicitConnect,
                                requiresManualVerification(loginState), isAuthenticated(loginState), isLoggedIn(ctx), haltedByCredential)
                    }
                    return result
                })
                .state(STATE_CHECK_CONNECTION, { builder -> builder
                        .guard({ ctx ->
                            if (ctx.getDispatcher().isServerConnected()) {
                                return false
                            }
                            def phase = ctx.getGameModel()?.getLoginState()?.getPhase()
                            if (phase == LoginState.Phase.LOGIN_SUCCESS || phase == LoginState.Phase.REDIRECTING
                                    || phase == LoginState.Phase.AGENT_CONNECTED) {
                                return false
                            }
                            return true
                        })
                        .action({ ctx ->
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            def gateway = resolveGatewayForAttempt(ctx, String.valueOf(settings?.targetGateway ?: ""))
                            if (gateway != null) {
                                ctx.getPersistentData().put("gatewayHost", gateway.host)
                                ctx.getPersistentData().put("gatewayPort", gateway.port)
                                ctx.getPersistentData().put("loginLastGatewayRaw", String.valueOf(settings?.targetGateway ?: ""))
                                logInfoCtx(ctx, "CHECK_CONNECTION resolved gateway -> {}:{} (raw='{}')",
                                        gateway.host, gateway.port, String.valueOf(settings?.targetGateway ?: ""))
                            } else {
                                ctx.getPersistentData().remove("gatewayHost")
                                ctx.getPersistentData().remove("gatewayPort")
                                logWarnCtx(ctx, "CHECK_CONNECTION gateway parse failed (raw='{}'), skipping connect",
                                        String.valueOf(settings?.targetGateway ?: ""))
                            }
                        })
                        .nextState(STATE_CHECK_CONNECTION_ROUTE)
                        .targetState(STATE_POST_CHECK_CONNECTION)
                })
                .conditionalState(STATE_CHECK_CONNECTION_ROUTE, { cb ->
                    cb.when({ ctx ->
                        def h = ctx.getPersistentData().get("gatewayHost")
                        h == null || String.valueOf(h).trim().isEmpty()
                    }, STATE_PARK_MISSING_PREREQS)
                            .when({ ctx ->
                        def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                        boolean explicitConnect = Boolean.TRUE.equals(ctx.getPersistentData().get("explicitConnectRequested"))
                        settings != null && !toBool(settings?.autoLogin, false) && !explicitConnect
                    }, STATE_PARK_MANUAL_CONNECT)
                            .defaultTo(STATE_CONNECT_TO_GATEWAY)
                })
                .exitState(STATE_PARK_MISSING_PREREQS, { eb ->
                    eb.beforeExit({ ctx ->
                        emitEnginePhase(ctx, PHASE_MISSING_GATEWAY, "MissingRequirement", null)
                    })
                })
                .exitState(STATE_PARK_MANUAL_CONNECT, { eb ->
                    eb.beforeExit({ ctx ->
                        emitEnginePhase(ctx, PHASE_PENDING_MANUAL_CONNECT, "ReadyToConnect", null)
                    })
                })
                .conditionalState(STATE_POST_CHECK_CONNECTION, { cb ->
                    cb.when({ ctx ->
                        def phase = ctx.getGameModel()?.getLoginState()?.getPhase()
                        boolean midRedirect = phase == LoginState.Phase.LOGIN_SUCCESS || phase == LoginState.Phase.REDIRECTING
                                || phase == LoginState.Phase.AGENT_CONNECTED
                        midRedirect && !ctx.getDispatcher().isServerConnected()
                    }, STATE_WAIT_FOR_LOGIN_RESPONSE)
                            .when({ ctx -> ctx.getDispatcher().isServerConnected() }, STATE_CONNECTED_RESUME)
                            .defaultTo(STATE_RETRY_DELAY)
                })
                .conditionalState(STATE_CONNECTED_RESUME, { cb ->
                    cb.when({ ctx ->
                        def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                        def ls = ctx.getGameModel()?.getLoginState()
                        loginPrereqsForGateway(settings, ls, ctx)
                    }, STATE_PAUSE_BEFORE_GATEWAY_LOGIN)
                            .when({ ctx -> hasAgents(ctx.getGameModel()?.getLoginState()) }, STATE_PARK_MISSING_LOGIN)
                            .defaultTo(STATE_RESEND_AGENT_LIST_REQUEST)
                })
                .state(STATE_RESEND_AGENT_LIST_REQUEST, { builder -> builder
                        .guard({ ctx -> true })
                        .action({ ctx ->
                            resetRetry(ctx)
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_DEADLINE_MS, System.currentTimeMillis() + effectiveAgentWaitTimeoutMs(settings))
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_TIMED_OUT, false)
                            emitEnginePhase(ctx, PHASE_WAITING_FOR_AGENTS, "WaitingForAgents", null)
                            logInfoCtx(ctx, "Already connected to gateway; (re-)requesting agent list")
                            if (!requestAgentList(ctx, settings, true)) {
                                ctx.getPersistentData().put(KEY_AGENT_WAIT_DEADLINE_MS, System.currentTimeMillis() - 1L)
                            }
                        })
                        .nextState(STATE_WAIT_FOR_AGENTS)
                        .targetState(STATE_RETRY_DELAY)
                })
                .state(STATE_PARK_MISSING_LOGIN, { builder -> builder
                        .guard({ ctx -> true })
                        .action({ ctx ->
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            def loginState = ctx.getGameModel()?.getLoginState()
                            if (settings == null) return
                            String preflight = evaluatePreflightStatus(settings, loginState)
                            if ("MISSING_CREDENTIALS".equals(preflight)) {
                                ctx.getPersistentData().put(KEY_USER_RESUME_REQUIRED, true)
                                emitEnginePhase(ctx, PHASE_MISSING_CREDENTIALS, "MissingRequirement", null)
                            } else if ("MISSING_AGENT_SERVER".equals(preflight)) {
                                ctx.getPersistentData().put(KEY_USER_RESUME_REQUIRED, true)
                                emitEnginePhase(ctx, PHASE_MISSING_AGENT_SERVER, "MissingRequirement", null)
                            } else if ("SERVER_INSPECTION".equals(preflight)) {
                                emitEnginePhase(ctx, PHASE_SERVER_INSPECTION, "ServerInspection",
                                        "Target server is under inspection. Retrying automatically.")
                            }
                            logInfoCtx(ctx, "Gateway session holds agent list; waiting for login prerequisites")
                        })
                        .nextState(null)
                        .targetState(STATE_RETRY_DELAY)
                })
                .state(STATE_CONNECT_TO_GATEWAY, { builder -> builder
                        .guard({ ctx -> ctx.getPersistentData().get("gatewayHost") != null })
                        .action({ ctx ->
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            if (settings instanceof Map) {
                                beginAttemptSnapshot(ctx, settings)
                            }
                            String host = normalizeHostForSocket((String) ctx.getPersistentData().get("gatewayHost"))
                            int port = (int) ctx.getPersistentData().get("gatewayPort")
                            emitEnginePhase(ctx, "CONNECTING_GATEWAY", "Connecting", null)
                            logInfoCtx(ctx, "CONNECT_TO_GATEWAY attempting -> {}:{} (alreadyConnected={})",
                                    host, port, ctx.getDispatcher().isServerConnected())
                            applyGatewayHandshakeHintsToProxy(ctx, settings instanceof Map ? (Map) settings : [:])
                            ctx.getDispatcher().setClientlessMode(true)
                            ctx.getDispatcher().connect(host, port)
                            logInfoCtx(ctx, "CONNECT_TO_GATEWAY result connected={}",
                                    ctx.getDispatcher().isServerConnected())
                        })
                        .nextState(STATE_WAIT_FOR_CONNECTION)
                        .targetState(STATE_RETRY_DELAY)
                })
                .state(STATE_WAIT_FOR_CONNECTION, { builder -> builder
                        .guard({ ctx -> ctx.getDispatcher().isServerConnected() })
                        .action({ ctx ->
                            resetRetry(ctx)
                            markGatewayConnectSuccess(ctx)
                            ctx.getPersistentData().remove(KEY_LAST_GATEWAY_LOGIN_REQUEST_MS)
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_DEADLINE_MS, System.currentTimeMillis() + effectiveAgentWaitTimeoutMs(settings))
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_TIMED_OUT, false)
                            emitEnginePhase(ctx, PHASE_WAITING_FOR_AGENTS, "WaitingForAgents", null)
                            logInfoCtx(ctx, "Gateway connected, requesting agent list")
                            if (!requestAgentList(ctx, settings, true)) {
                                ctx.getPersistentData().put(KEY_AGENT_WAIT_DEADLINE_MS, System.currentTimeMillis() - 1L)
                            }
                        })
                        .nextState(STATE_WAIT_FOR_AGENTS)
                        .targetState(STATE_RETRY_DELAY)
                })
                .state(STATE_WAIT_FOR_AGENTS, { builder -> builder
                        .guard({ ctx ->
                            if (Boolean.TRUE.equals(ctx.getPersistentData().get(KEY_LOGIN_HALTED_CREDENTIAL))) {
                                return false
                            }
                            hasAgentListReceived(ctx.getGameModel()?.getLoginState())
                        })
                        .action({ ctx ->
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_TIMED_OUT, false)
                            ctx.getPersistentData().put(KEY_AGENT_LIST_CACHE_AT_MS, System.currentTimeMillis())
                            emitEnginePhase(ctx, LoginState.Phase.AGENTS_RECEIVED.name(), "AgentsReceived", null)
                            logInfoCtx(ctx, "Agent list received, sending login request")
                        })
                        .nextState(STATE_PAUSE_BEFORE_GATEWAY_LOGIN)
                        .targetState(STATE_CHECK_AGENT_WAIT_TIMEOUT)
                })
                .delayState(STATE_PAUSE_BEFORE_GATEWAY_LOGIN, { builder -> builder
                        .delay(0)
                        .minDelay(0)
                        .delayAction({ ctx ->
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            long ms = effectiveGatewayLoginPauseAfterAgentListMs(settings)
                            if (ms > 0L) {
                                emitEnginePhase(ctx, PHASE_GATEWAY_LOGIN_PAUSE, "GatewayLoginPause", null,
                                        [pauseMs: Long.valueOf(ms)])
                            }
                        })
                        .nextState(STATE_SEND_LOGIN_REQUEST)
                })
                .state(STATE_CHECK_AGENT_WAIT_TIMEOUT, { builder -> builder
                        .guard({ ctx -> isAgentWaitTimedOut(ctx) })
                        .action({ ctx ->
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_TIMED_OUT, true)
                            emitEnginePhase(ctx, PHASE_WAITING_FOR_AGENTS_TIMEOUT, "WaitingForAgentsTimeout", null)
                            def loginState = ctx.getGameModel()?.getLoginState()
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            if (loginState != null) {
                                loginState.setFailureReason("Agent list timeout")
                            }
                            logWarnCtx(ctx, "Agent list wait timed out after {} ms; entering retry flow", effectiveAgentWaitTimeoutMs(settings))
                        })
                        .nextState(STATE_RETRY_DELAY)
                        .targetState(STATE_WAIT_FOR_AGENTS_RECHECK)
                })
                .delayState(STATE_WAIT_FOR_AGENTS_RECHECK, { builder -> builder
                        .delay(500)
                        .nextState(STATE_WAIT_FOR_AGENTS)
                })
                .state(STATE_SEND_LOGIN_REQUEST, { builder -> builder
                        .guard({ ctx ->
                            def settings = currentAttemptSettings(ctx, settingsProvider)
                            def loginState = ctx.getGameModel()?.getLoginState()
                            if (settings == null) return false
                            if (Boolean.TRUE.equals(ctx.getPersistentData().get(KEY_LOGIN_HALTED_CREDENTIAL))) {
                                return false
                            }
                            if (Boolean.TRUE.equals(ctx.getPersistentData().get(KEY_LOGIN_IN_PROGRESS))) {
                                return false
                            }
                            try {
                                if (loginState != null && loginState.getPhase() == LoginState.Phase.LOGIN_SENT) {
                                    return false
                                }
                            } catch (Exception ignored) {
                            }
                            String preflight = evaluatePreflightStatus(settings, loginState)
                            if ("MISSING_CREDENTIALS".equals(preflight)) {
                                ctx.getPersistentData().put(KEY_USER_RESUME_REQUIRED, true)
                                emitEnginePhase(ctx, PHASE_MISSING_CREDENTIALS, "MissingRequirement", null)
                                return false
                            }
                            if ("MISSING_AGENT_SERVER".equals(preflight)) {
                                ctx.getPersistentData().put(KEY_USER_RESUME_REQUIRED, true)
                                emitEnginePhase(ctx, PHASE_MISSING_AGENT_SERVER, "MissingRequirement", null)
                                return false
                            }
                            if ("SERVER_INSPECTION".equals(preflight)) {
                                emitEnginePhase(ctx, PHASE_SERVER_INSPECTION, "ServerInspection",
                                        "Target server is under inspection. Retrying automatically.")
                                return false
                            }
                            return true
                        })
                        .action({ ctx ->
                            ctx.getPersistentData().remove(KEY_USER_RESUME_REQUIRED)
                            def settings = currentAttemptSettings(ctx, settingsProvider)
                            if (settings == null) return
                            String username = String.valueOf(settings.username ?: "")
                            String password = String.valueOf(settings.password ?: "")
                            synchronized (ctx) {
                                if (Boolean.TRUE.equals(ctx.getPersistentData().get(KEY_LOGIN_IN_PROGRESS))) {
                                    logWarnCtx(ctx, "Login request ignored: already in progress")
                                    return
                                }
                                long gateNow = System.currentTimeMillis()
                                if (!allowGatewayLoginRequestNow(ctx, settings, gateNow)) {
                                    logWarnCtx(ctx, "Gateway 0x6102 deferred: min interval {} ms (machine={}, serverConnected={})",
                                            effectiveGatewayLoginMinIntervalMs(settings), ctx.getMachineName(), safeIsServerConnected(ctx))
                                    return
                                }
                                ctx.getPersistentData().put(KEY_LOGIN_IN_PROGRESS, true)
                                ctx.getPersistentData().put(KEY_LAST_GATEWAY_LOGIN_REQUEST_MS, gateNow)
                                try {
                                    byte locale = effectiveGatewayLocale(settings)
                                    short agentId = parseAgentId(String.valueOf(settings.targetAgent ?: ""))
                                    String charsetName = String.valueOf(settings.loginCharset ?: "windows-1252")
                                    logInfoCtx(ctx, "Sending gateway 0x6102 for user: {} (locale=0x{}, agentId={}, charset={}, serverConnected={})",
                                            redactForLog(username), String.format("%02X", locale & 0xFF), (int) agentId & 0xFFFF, charsetName, safeIsServerConnected(ctx))
                                    ctx.getGameModel()?.getLoginState()?.setPhase(LoginState.Phase.LOGIN_SENT)
                                    emitEnginePhase(ctx, LoginState.Phase.LOGIN_SENT.name(), "LoginSent", null)
                                    long now = System.currentTimeMillis()
                                    ctx.getPersistentData().put(KEY_LOGIN_RESPONSE_DEADLINE_MS, now + effectiveLoginResponseTimeoutMs(settings))
                                    ctx.getPersistentData().put(KEY_AGENT_AUTH_DEADLINE_MS, now + effectiveAgentAuthTimeoutMs(settings))
                                    sendLoginRequest(ctx, locale, username, password, agentId, charsetName)
                                } catch (Throwable t) {
                                    ctx.getPersistentData().put(KEY_LOGIN_IN_PROGRESS, false)
                                    ctx.getPersistentData().remove(KEY_LAST_GATEWAY_LOGIN_REQUEST_MS)
                                    if (t instanceof Error && !(t instanceof Exception)) {
                                        throw (Error) t
                                    }
                                    if (t instanceof Exception) {
                                        throw (Exception) t
                                    }
                                    throw new RuntimeException(t)
                                }
                            }
                        })
                        .nextState(STATE_WAIT_FOR_LOGIN_RESPONSE)
                        .targetState(STATE_RETRY_DELAY)
                })
                .state(STATE_WAIT_FOR_LOGIN_RESPONSE, { builder -> builder
                        .guard({ ctx ->
                            def s = snapshot(ctx)
                            def phase = s.phase
                            if (phase == LoginState.Phase.LOGIN_SUCCESS || phase == LoginState.Phase.REDIRECTING
                                    || phase == LoginState.Phase.AGENT_CONNECTED
                                    || phase == LoginState.Phase.AUTHENTICATED) {
                                return true
                            }
                            return s.loggedIn
                        })
                        .action({ ctx ->
                            def s = snapshot(ctx)
                            logInfoCtx(ctx, "Gateway login response received (phase={})", s.phase)
                        })
                        .nextState(STATE_WAIT_FOR_AGENT_SERVER_CONNECTION)
                        .targetState(STATE_SUBMIT_GATEWAY_IMAGE_CODE)
                })
                .state(STATE_SUBMIT_GATEWAY_IMAGE_CODE, { builder -> builder
                        .guard({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            return loginState?.getPhase() == LoginState.Phase.WAIT_FOR_CAPTCHA && safeIsServerConnected(ctx)
                        })
                        .action({ ctx ->
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, false)
                            String answer = settings != null ? String.valueOf(settings.passcode ?: "").trim() : ""
                            emitEnginePhase(ctx, PHASE_WAIT_FOR_CAPTCHA, "GatewayImageCode",
                                    "Gateway image code challenge; answer from login passcode field (UTF-16, may be empty)",
                                    [interactive: true, gatewayImageCode: true])
                            sendGatewayImageCodeAnswer(ctx, answer)
                            def ls = ctx.getGameModel()?.getLoginState()
                            if (ls != null) {
                                ls.setPhase(LoginState.Phase.LOGIN_SENT)
                                ls.setFailureReason(null)
                            }
                            long now = System.currentTimeMillis()
                            def s = resolveRuntimeSettings(ctx, settingsProvider, true)
                            ctx.getPersistentData().put(KEY_LOGIN_RESPONSE_DEADLINE_MS, now + effectiveLoginResponseTimeoutMs(s))
                            logInfoCtx(ctx, "Sent gateway 0x6323 image code answer (chars={})", answer.length())
                        })
                        .nextState(STATE_WAIT_FOR_LOGIN_RECHECK)
                        .targetState(STATE_CHECK_LOGIN_FAILED_IMMEDIATE)
                })
                .state(STATE_WAIT_FOR_AGENT_SERVER_CONNECTION, { builder -> builder
                        .guard({ ctx ->
                            def s = snapshot(ctx)
                            def phase = s.phase
                            return phase == LoginState.Phase.AGENT_CONNECTED
                                    || phase == LoginState.Phase.AUTHENTICATED
                                    || s.loggedIn
                        })
                        .action({ ctx -> })
                        .nextState(STATE_WAIT_FOR_AGENT_AUTH)
                        .targetState(STATE_CHECK_AGENT_SERVER_CONNECTION_FAILURE)
                })
                .state(STATE_CHECK_AGENT_SERVER_CONNECTION_FAILURE, { builder -> builder
                        .guard({ ctx ->
                            def s = snapshot(ctx)
                            if (s.phase == LoginState.Phase.FAILED) {
                                return true
                            }
                            if (s.loggedIn || s.phase == LoginState.Phase.AGENT_CONNECTED || s.phase == LoginState.Phase.AUTHENTICATED) {
                                return false
                            }
                            if (isAgentAuthTimedOut(ctx)) {
                                return true
                            }
                            def ev = readLatestNetworkFailure(ctx)
                            return ev != null
                        })
                        .action({ ctx ->
                            def s = snapshot(ctx)
                            def loginState = s.loginState
                            def ev = readLatestNetworkFailure(ctx)
                            if (loginState != null && ev != null) {
                                loginState.setFailureReason(String.valueOf(ev.reason ?: "Agent connection failed"))
                            } else if (loginState != null && isAgentAuthTimedOut(ctx) && loginState.getPhase() != LoginState.Phase.FAILED) {
                                loginState.setFailureReason("Agent server connection timeout")
                            }
                        })
                        .nextState(STATE_RETRY_DELAY)
                        .targetState(STATE_WAIT_FOR_AGENT_SERVER_CONNECTION_RECHECK)
                })
                .delayState(STATE_WAIT_FOR_AGENT_SERVER_CONNECTION_RECHECK, { builder -> builder
                        .delay(500)
                        .nextState(STATE_WAIT_FOR_AGENT_SERVER_CONNECTION)
                })
                .state(STATE_CHECK_LOGIN_FAILED_IMMEDIATE, { builder -> builder
                        .guard({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            return loginState?.getPhase() == LoginState.Phase.FAILED || loginState?.getGatewayResultCode() != null
                        })
                        .action({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            String failureClass = classifyFailure(loginState, ctx)
                            ctx.getPersistentData().remove(KEY_LOGIN_RESPONSE_DEADLINE_MS)
                            emitEnginePhase(ctx, LoginState.Phase.FAILED.name(), "GatewayLoginFailed",
                                    String.valueOf(loginState?.getFailureReason() ?: ""),
                                    [failureClass: failureClass, gatewayResultCode: parseGatewayFailureCode(loginState)])
                        })
                        .nextState(STATE_RETRY_DELAY)
                        .targetState(STATE_CHECK_LOGIN_TIMEOUT)
                })
                .state(STATE_CHECK_LOGIN_TIMEOUT, { builder -> builder
                        .guard({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            if (loginState?.getPhase() == LoginState.Phase.FAILED) {
                                return true
                            }
                            return isLoginResponseTimedOut(ctx) && loginState?.getPhase() == LoginState.Phase.LOGIN_SENT
                        })
                        .action({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            if (loginState?.getPhase() == LoginState.Phase.FAILED) {
                                logWarnCtx(ctx, "Gateway login failed: {}", loginState?.getFailureReason() ?: "")
                            } else {
                                if (loginState != null) {
                                    loginState.setFailureReason("Gateway login response timeout")
                                }
                                logWarnCtx(ctx, "Gateway login response timed out after {} ms", effectiveLoginResponseTimeoutMs(settings))
                            }
                        })
                        .nextState(STATE_RETRY_DELAY)
                        .targetState(STATE_WAIT_FOR_LOGIN_RECHECK)
                })
                .delayState(STATE_WAIT_FOR_LOGIN_RECHECK, { builder -> builder
                        .delay(500)
                        .nextState(STATE_WAIT_FOR_LOGIN_RESPONSE)
                })
                .state(STATE_WAIT_FOR_AGENT_AUTH, { builder -> builder
                        .guard({ ctx ->
                            def s = snapshot(ctx)
                            return isAuthenticated(s.loginState) || s.loggedIn
                        })
                        .action({ ctx -> })
                        .nextState(STATE_WAIT_FOR_CHARACTER)
                        .targetState(STATE_CHECK_AGENT_AUTH_TIMEOUT)
                })
                .state(STATE_CHECK_AGENT_AUTH_TIMEOUT, { builder -> builder
                        .guard({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            if (loginState?.getPhase() == LoginState.Phase.FAILED) {
                                return true
                            }
                            if (!isAgentAuthTimedOut(ctx)) {
                                return false
                            }
                            if (isLoggedIn(ctx)) {
                                return false
                            }
                            return !isAuthenticated(loginState)
                        })
                        .action({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            if (loginState?.getPhase() != LoginState.Phase.FAILED && loginState != null) {
                                loginState.setFailureReason("Agent authentication timeout")
                            }
                            logWarnCtx(ctx, "Agent authentication timed out after {} ms (phase={})",
                                    effectiveAgentAuthTimeoutMs(settings), loginState?.getPhase())
                        })
                        .nextState(STATE_RETRY_DELAY)
                        .targetState(STATE_WAIT_FOR_AGENT_AUTH_RECHECK)
                })
                .delayState(STATE_WAIT_FOR_AGENT_AUTH_RECHECK, { builder -> builder
                        .delay(500)
                        .nextState(STATE_WAIT_FOR_AGENT_AUTH)
                })
                .state(STATE_WAIT_FOR_CHARACTER, { builder -> builder
                        .guard({ ctx ->
                            if (isLoggedIn(ctx)) {
                                return true
                            }
                            def loginState = ctx.getGameModel()?.getLoginState()
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            if (loginState?.getPhase() == LoginState.Phase.LOADING_ENVIRONMENT) {
                                emitEnginePhase(ctx, PHASE_LOADING_ENVIRONMENT, "LoadingEnvironment", null)
                                return false
                            }
                            if (isAuthenticated(loginState)) {
                                def availableCharacters = loginState?.getAvailableCharacterNames() ?: []
                                String selectedCharacter = String.valueOf(settings?.selectedCharacter ?: "").trim()
                                int slotBase = toInt(settings?.characterSlotBase, 0)
                                int selectedSlotRaw = toInt(settings?.selectedCharacterSlot, -1)
                                boolean strict = toBool(settings?.characterSelectionStrictMode, false)
                                String slotName = null
                                if (selectedSlotRaw >= slotBase) {
                                    int idx = selectedSlotRaw - slotBase
                                    if (idx >= 0 && idx < availableCharacters.size()) {
                                        slotName = String.valueOf(availableCharacters.get(idx))
                                    } else if (!selectedCharacter.isEmpty()) {
                                        logWarnCtx(ctx, "Selected character slot {} out of range, falling back to name", selectedSlotRaw)
                                    } else {
                                        loginState.setFailureReason("CHARACTER_SELECTION_UNAVAILABLE")
                                        loginState.setPhase(LoginState.Phase.FAILED)
                                        return false
                                    }
                                }
                                if (!availableCharacters.isEmpty() && !selectedCharacter.isEmpty()) {
                                    boolean nameOk = availableCharacters.contains(selectedCharacter)
                                    boolean slotOk = (slotName == null) || selectedCharacter.equals(slotName)
                                    if (!nameOk || (strict && !slotOk)) {
                                        loginState.setFailureReason("CHARACTER_SELECTION_UNAVAILABLE")
                                        loginState.setPhase(LoginState.Phase.FAILED)
                                        return false
                                    }
                                }
                                if (!availableCharacters.isEmpty() && isBlank(settings?.selectedCharacter)) {
                                    emitEnginePhase(ctx, PHASE_MISSING_CHARACTER_SELECTION, "MissingRequirement", null)
                                    return false
                                }
                            }
                            return false
                        })
                        .action({ ctx ->
                            resetRetry(ctx)
                            ctx.getPersistentData().put(KEY_LOGIN_IN_PROGRESS, false)
                            ctx.getPersistentData().remove(KEY_LOGIN_HALTED_CREDENTIAL)
                            ctx.getPersistentData().remove(KEY_ATTEMPT_LOGIN_SETTINGS)
                            ctx.getPersistentData().remove(KEY_INTERACTIVE_WAIT_UNTIL_MS)
                            ctx.getPersistentData().remove(KEY_RETRY_ATTEMPT_ID)
                            ctx.getPersistentData().remove(KEY_RETRY_UNTIL_MS)
                            ctx.getPersistentData().remove("explicitConnectRequested")
                            emitEnginePhase(ctx, LoginState.Phase.AUTHENTICATED.name(), "Authenticated", null)
                            logInfoCtx(ctx, "Login/authentication successful")
                        })
                        .nextState(null)
                        .targetState(STATE_CHECK_CHARACTER_WAIT_TIMEOUT)
                })
                .state(STATE_CHECK_CHARACTER_WAIT_TIMEOUT, { builder -> builder
                        .guard({ ctx ->
                            if (isLoggedIn(ctx)) {
                                return false
                            }
                            if (!isAgentAuthTimedOut(ctx)) {
                                return false
                            }
                            def loginState = ctx.getGameModel()?.getLoginState()
                            return isAuthenticated(loginState)
                        })
                        .action({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            if (loginState != null) {
                                loginState.setFailureReason("Character selection or in-game timeout")
                            }
                            logWarnCtx(ctx, "Post-auth wait timed out after {} ms (phase={})", effectiveAgentAuthTimeoutMs(settings),
                                    ctx.getGameModel()?.getLoginState()?.getPhase())
                        })
                        .nextState(STATE_RETRY_DELAY)
                        .targetState(STATE_WAIT_FOR_CHARACTER_RECHECK)
                })
                .delayState(STATE_WAIT_FOR_CHARACTER_RECHECK, { builder -> builder
                        .delay(500)
                        .nextState(STATE_WAIT_FOR_CHARACTER)
                })
                .state(STATE_RETRY_DELAY, { builder -> builder
                        .guard({ ctx -> true })
                        .action({ ctx ->
                            ctx.getPersistentData().put(KEY_LOGIN_IN_PROGRESS, false)
                            ctx.getPersistentData().remove(KEY_ATTEMPT_LOGIN_SETTINGS)
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            def s = snapshot(ctx)
                            def loginState = s.loginState

                            // Chain of responsibility:
                            // (a) interactive waits (captcha/passcode)
                            if (handleInteractivePasscodeWait(ctx, loginState, s, settings)) return
                            // (b) queue wait
                            if (handleQueueWait(ctx, loginState)) return
                            // (c) missing prereqs / user-resume-required
                            if (handleMissingPrereq(ctx, loginState)) return

                            String failureClass = classifyFailure(loginState, ctx)
                            logInfoCtx(ctx, "Failure classified as {}", failureClass)

                            // (d) terminal halts / special cases
                            if (handleTerminalFailure(ctx, loginState, failureClass, settings)) return

                            // (e) schedule retry
                            scheduleRetry(ctx, loginState, s, failureClass, settings)
                        })
                        .delay(0)
                        .nextState(STATE_WAIT_RETRY_DELAY)
                })
                .state(STATE_WAIT_RETRY_DELAY, { builder -> builder
                        .guard({ ctx ->
                            try {
                                def retryAttemptRaw = ctx.getPersistentData().get(KEY_RETRY_ATTEMPT_ID)
                                long retryAttemptId = (retryAttemptRaw instanceof Number) ? ((Number) retryAttemptRaw).longValue() : -1L
                                if (retryAttemptId > 0L && retryAttemptId != currentAttemptId(ctx)) {
                                    return true
                                }
                                def retryUntilRaw = ctx.getPersistentData().get(KEY_RETRY_UNTIL_MS)
                                if (!(retryUntilRaw instanceof Number)) {
                                    return true
                                }
                                long retryUntilMs = ((Number) retryUntilRaw).longValue()
                                return retryUntilMs <= 0L || System.currentTimeMillis() >= retryUntilMs
                            } catch (Exception e) {
                                logErrorThrottled(ctx, "LOGIN_WAIT_RETRY_DELAY_01", e)
                                return true
                            }
                        })
                        .action({ ctx ->
                            ctx.getPersistentData().remove(KEY_RETRY_UNTIL_MS)
                            ctx.getPersistentData().remove(KEY_RETRY_ATTEMPT_ID)
                        })
                        .nextState(null)
                        .targetState(STATE_WAIT_RETRY_DELAY_RECHECK)
                })
                .delayState(STATE_WAIT_RETRY_DELAY_RECHECK, { builder -> builder
                        .delay(250)
                        .nextState(STATE_WAIT_RETRY_DELAY)
                })
                .build()

        context.getWorkflowRegistry().registerCycle(cycle)
        log.info("Login cycle registered successfully")
    }

    private boolean isLoggedIn(def ctx) {
        try {
            def trainer = ctx.getGameModel().getTrainer()
            return trainer != null && trainer.getUniqueId() > 0
        } catch (Exception e) { return false }
    }

    private Map volatileState(def ctx) {
        def existing = ctx?.getPersistentData()?.get(KEY_VOLATILE_STATE)
        if (existing instanceof ConcurrentHashMap) return (Map) existing
        def created = new ConcurrentHashMap<String, Object>()
        ctx?.getPersistentData()?.put(KEY_VOLATILE_STATE, created)
        if (!created.containsKey(KEY_VOLATILE_RESET_TOKEN)) {
            created.put(KEY_VOLATILE_RESET_TOKEN, 0L)
        }
        return created
    }

    private long bumpResetToken(def ctx) {
        Map state = volatileState(ctx)
        return ((Long) state.compute(KEY_VOLATILE_RESET_TOKEN, { k, v ->
            long cur = (v instanceof Number) ? ((Number) v).longValue() : 0L
            return cur + 1L
        })).longValue()
    }

    private boolean putVolatileWithToken(def ctx, String key, Object value, long expectedToken) {
        Map state = volatileState(ctx)
        def outcome = state.compute(key, { k, existing ->
            long current = toLong(state.get(KEY_VOLATILE_RESET_TOKEN), 0L)
            if (current != expectedToken) return existing
            return value
        })
        long current = toLong(state.get(KEY_VOLATILE_RESET_TOKEN), 0L)
        return current == expectedToken
    }

    private void clearVolatileAttributes(def ctx) {
        Map state = volatileState(ctx)
        long next = toLong(state.get(KEY_VOLATILE_RESET_TOKEN), 0L) + 1L
        state.clear()
        state.put(KEY_VOLATILE_RESET_TOKEN, next)
    }

    private void logInfoCtx(def ctx, String template, Object... args) {
        if (!log.isInfoEnabled()) return
        Object[] prefixed = new Object[args.length + 2]
        prefixed[0] = ctx?.getMachineName()
        prefixed[1] = Thread.currentThread().getName()
        for (int i = 0; i < args.length; i++) prefixed[i + 2] = args[i]
        log.info("[machine={}] [thread={}] " + template, prefixed)
    }

    private void logWarnCtx(def ctx, String template, Object... args) {
        Object[] prefixed = new Object[args.length + 2]
        prefixed[0] = ctx?.getMachineName()
        prefixed[1] = Thread.currentThread().getName()
        for (int i = 0; i < args.length; i++) prefixed[i + 2] = args[i]
        log.warn("[machine={}] [thread={}] " + template, prefixed)
    }

    private void logErrorThrottled(def ctx, String siteId, Exception e) {
        Map throttle = (Map) ctx?.getPersistentData()?.get(KEY_ERROR_THROTTLE)
        if (!(throttle instanceof Map)) {
            throttle = new LinkedHashMap<String, Long>(128, 0.75f, true)
            ctx?.getPersistentData()?.put(KEY_ERROR_THROTTLE, throttle)
        }
        long now = System.currentTimeMillis()
        String key = String.valueOf(ctx?.getMachineName()) + "|" + siteId + "|" + e.getClass().getSimpleName()
        Long last = (Long) throttle.get(key)
        if (last == null || (now - last.longValue()) > 60000L) {
            logWarnCtx(ctx, "[{}] {}", siteId, e.getMessage())
            throttle.put(key, now)
        }
        if (throttle.size() > 1000) {
            def it = throttle.keySet().iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
            }
        }
    }

    private short parseAgentId(String targetAgent) {
        if (!targetAgent) return (short) 0
        try { return Short.parseShort(targetAgent) }
        catch (NumberFormatException e) { log.warn("Invalid agent ID format: {}", targetAgent); return (short) 0 }
    }

    private Map parseGateway(String raw) {
        List<Map> parsed = parseGatewayEndpoints(raw)
        return parsed.isEmpty() ? null : parsed.get(0)
    }

    private List<Map> parseGatewayEndpoints(String raw) {
        if (!raw) {
            log.warn("No target gateway configured")
            return []
        }
        List<Map> endpoints = []
        String.valueOf(raw).split(",").each { token ->
            def parsed = parseGatewayEndpoint(String.valueOf(token ?: ""))
            if (parsed != null) {
                endpoints.add(parsed)
            }
        }
        if (endpoints.isEmpty()) {
            log.warn("No valid target gateways configured (raw='{}')", String.valueOf(raw))
        }
        return endpoints
    }

    private Map parseGatewayEndpoint(String token) {
        String value = String.valueOf(token ?: "").trim()
        if (value.isEmpty()) return null
        try {
            if (value.startsWith("[") && value.contains("]")) {
                int close = value.indexOf(']')
                String host = value.substring(1, close).trim()
                String remainder = value.substring(close + 1).trim()
                int port = 15779
                if (remainder.startsWith(":")) {
                    port = Integer.parseInt(remainder.substring(1).trim())
                } else if (!remainder.isEmpty()) {
                    return null
                }
                return [host: host, port: port]
            }
            int firstColon = value.indexOf(':')
            int lastColon = value.lastIndexOf(':')
            if (firstColon >= 0 && firstColon == lastColon) {
                String host = value.substring(0, firstColon).trim()
                int port = Integer.parseInt(value.substring(firstColon + 1).trim())
                return [host: host, port: port]
            }
            if (firstColon >= 0 && firstColon != lastColon) {
                // Ambiguous unbracketed IPv6+port; treat as host-only endpoint.
                return [host: value, port: 15779]
            }
            return [host: value, port: 15779]
        } catch (NumberFormatException e) {
            log.warn("Invalid target gateway format '{}', expected host:port or [ipv6]:port", value)
            return null
        }
    }

    private String normalizeHostForSocket(String host) {
        String value = String.valueOf(host ?: "").trim()
        if (value.startsWith("[") && value.endsWith("]") && value.length() > 2) {
            return value.substring(1, value.length() - 1)
        }
        return value
    }

    private Map resolveGatewayForAttempt(def ctx, String raw) {
        List<Map> endpoints = parseGatewayEndpoints(raw)
        if (endpoints.isEmpty()) return null
        int index = nextGatewayIndex(ctx, endpoints.size())
        Map selected = endpoints.get(index)
        ctx.getPersistentData().put(KEY_GATEWAY_ENDPOINTS, endpoints)
        ctx.getPersistentData().put(KEY_GATEWAY_ENDPOINT_INDEX, index)
        return selected
    }

    private int nextGatewayIndex(def ctx, int size) {
        if (size <= 1) return 0
        long now = System.currentTimeMillis()
        int lastKnownGood = toInt(ctx?.getPersistentData()?.get(KEY_GATEWAY_KNOWN_GOOD_INDEX), -1)
        long connectedAt = toLong(ctx?.getPersistentData()?.get(KEY_GATEWAY_CONNECTED_AT_MS), 0L)
        long resolvedAt = toLong(ctx?.getPersistentData()?.get(KEY_GATEWAY_DNS_RESOLVED_AT_MS), 0L)
        boolean longSession = connectedAt > 0L && (now - connectedAt) > DNS_STICKY_TTL_MS
        boolean staleResolution = resolvedAt > 0L && (now - resolvedAt) > DNS_STICKY_TTL_MS
        if (lastKnownGood >= 0 && lastKnownGood < size && !(longSession || staleResolution)) {
            return lastKnownGood
        }
        int current = toInt(ctx?.getPersistentData()?.get(KEY_GATEWAY_ENDPOINT_INDEX), -1)
        if (current < 0) {
            return (int) (Math.abs(now) % size)
        }
        return (current + 1) % size
    }

    private void markGatewayConnectSuccess(def ctx) {
        int idx = toInt(ctx?.getPersistentData()?.get(KEY_GATEWAY_ENDPOINT_INDEX), -1)
        if (idx >= 0) {
            ctx.getPersistentData().put(KEY_GATEWAY_KNOWN_GOOD_INDEX, idx)
        }
        long now = System.currentTimeMillis()
        ctx.getPersistentData().put(KEY_GATEWAY_CONNECTED_AT_MS, now)
        ctx.getPersistentData().put(KEY_GATEWAY_DNS_RESOLVED_AT_MS, now)
    }

    private void markGatewayAttemptFailure(def ctx) {
        // force fresh endpoint selection on next retry tick
        ctx.getPersistentData().remove(KEY_GATEWAY_DNS_RESOLVED_AT_MS)
    }

    private boolean hasAgents(def loginState) {
        try {
            return loginState != null && loginState.getAgentList() != null && !loginState.getAgentList().isEmpty()
        } catch (Exception e) {
            return false
        }
    }

    private boolean hasAgentListReceived(def loginState) {
        try {
            if (hasAgents(loginState)) return true
            def phase = loginState?.getPhase()
            return phase == LoginState.Phase.AGENTS_RECEIVED
        } catch (Exception ignored) {
            return false
        }
    }

    private String evaluatePreflightStatus(def settings, def loginState) {
        if (settings == null) return "MISSING_CREDENTIALS"
        if (isBlank(settings.username) || isBlank(settings.password)) {
            return "MISSING_CREDENTIALS"
        }
        def agents = loginState?.getAgentList()
        if (agents == null || agents.isEmpty()) {
            return "SERVER_INSPECTION"
        }
        if (isBlank(settings.targetAgent)) {
            return "MISSING_AGENT_SERVER"
        }
        short selectedAgentId = parseAgentId(String.valueOf(settings.targetAgent ?: ""))
        def selected = agents.find { a ->
            try { return ((short) a.getId()) == selectedAgentId } catch (Exception ignored) { return false }
        }
        if (selected == null) {
            return "MISSING_AGENT_SERVER"
        }
        byte status = 1
        int online = 0
        int capacity = 0
        try { status = (byte) selected.getStatus() } catch (Exception ignored) {}
        try { online = (int) selected.getOnlineCount() } catch (Exception ignored) {}
        try { capacity = (int) selected.getCapacity() } catch (Exception ignored) {}
        boolean serverFull = capacity > 0 && online >= capacity
        if (!serverFull && status == 0) {
            return "SERVER_INSPECTION"
        }
        return "READY"
    }

    /** True when we can send 0x6102: agent list present plus username, password, and target shard id. */
    private boolean loginPrereqsForGateway(def settings, def loginState, def ctx) {
        if (Boolean.TRUE.equals(ctx?.getPersistentData()?.get(KEY_LOGIN_HALTED_CREDENTIAL))) {
            return false
        }
        return "READY".equals(evaluatePreflightStatus(settings, loginState))
    }

    private static String normalizeGatewayClientModule(Object v) {
        String m = String.valueOf(v != null ? v : "SR_Client").trim()
        m.isEmpty() ? "SR_Client" : m
    }

    /** Forwards locale / module / build to proxy before TCP connect (clientless 0x6100 + 0x2002 after gateway 0x2001). */
    private void applyGatewayHandshakeHintsToProxy(def ctx, Map settings) {
        try {
            def proxy = ctx?.getProxyConnection()
            if (proxy == null) return
            Map s = settings != null ? settings : [:]
            byte loc = effectiveGatewayLocale(s)
            String mod = normalizeGatewayClientModule(s.gatewayClientModule)
            int ver = toInt(s.gatewayClientVersion, 188)
            proxy.setGatewayHandshakeHints(loc, mod, ver)
        } catch (Exception e) {
            log.debug("setGatewayHandshakeHints skipped: {}", e.message)
        }
    }

    /**
     * Gateway locale byte (vSRO commonly 22). {@code 0} is treated as unset and coerced to 22 — explicit 0 breaks many gateways.
     */
    private byte effectiveGatewayLocale(def settings) {
        if (settings == null) return (byte) 22
        def raw = settings.locale
        if (raw == null) return (byte) 22
        int v = (raw instanceof Number) ? ((Number) raw).intValue() : toInt(String.valueOf(raw), 22)
        if (v == 0) return (byte) 22
        return (byte) (v & 0xFF)
    }

    private long effectiveGatewayLoginMinIntervalMs(def settings) {
        if (settings instanceof Map && settings.gatewayLoginMinIntervalMs instanceof Number) {
            return Math.max(0L, ((Number) settings.gatewayLoginMinIntervalMs).longValue())
        }
        String prop = System.getProperty("sokybot.login.request.minIntervalMs")
        if (prop != null && !prop.isBlank()) {
            try {
                return Math.max(0L, Long.parseLong(prop.trim()))
            } catch (NumberFormatException ignored) {
            }
        }
        return 2000L
    }

    /**
     * Pause after agent list before 0x6102. Runtime snapshot clamps 0–60s; if absent, system property
     * {@code sokybot.login.pauseAfterAgentListMs} then default {@code 1500} ms.
     */
    private long effectiveGatewayLoginPauseAfterAgentListMs(def settings) {
        if (settings instanceof Map && settings.gatewayLoginPauseAfterAgentListMs instanceof Number) {
            return Math.max(0L, Math.min(60000L, ((Number) settings.gatewayLoginPauseAfterAgentListMs).longValue()))
        }
        String prop = System.getProperty("sokybot.login.pauseAfterAgentListMs")
        if (prop != null && !prop.isBlank()) {
            try {
                return Math.max(0L, Math.min(60000L, Long.parseLong(prop.trim())))
            } catch (NumberFormatException ignored) {
            }
        }
        return 1500L
    }

    private boolean allowGatewayLoginRequestNow(def ctx, def settings, long nowMs) {
        long minIv = effectiveGatewayLoginMinIntervalMs(settings)
        if (minIv <= 0L) return true
        def lastRaw = ctx.getPersistentData().get(KEY_LAST_GATEWAY_LOGIN_REQUEST_MS)
        if (!(lastRaw instanceof Number)) return true
        return nowMs - ((Number) lastRaw).longValue() >= minIv
    }

    private boolean safeIsServerConnected(def ctx) {
        try {
            return ctx?.getDispatcher()?.isServerConnected()
        } catch (Exception ignored) {
            return false
        }
    }

    private boolean isAuthenticated(def loginState) {
        try {
            return loginState != null && loginState.getPhase() == LoginState.Phase.AUTHENTICATED
        } catch (Exception e) {
            return false
        }
    }

    private boolean requiresManualVerification(def loginState) {
        try {
            def phase = loginState?.getPhase()
            if (phase == LoginState.Phase.WAIT_FOR_CAPTCHA) return true
            String reason = String.valueOf(loginState?.getFailureReason() ?: "").toLowerCase()
            if (!reason) return false
            return reason.contains("captcha")
                    || reason.contains("image")
                    || reason.contains("verification")
                    || reason.contains("security")
        } catch (Exception e) {
            return false
        }
    }

    private boolean isPasscodeInputRequired(def loginState) {
        try {
            def phase = loginState?.getPhase()
            if (phase == LoginState.Phase.WAITING_FOR_PASSCODE
                    || phase == LoginState.Phase.WAIT_FOR_CAPTCHA
                    || phase == LoginState.Phase.PASSCODE_SUBMITTED) {
                return true
            }
            boolean allowStringDetection = true
            try {
                def raw = loginSettingsProvider?.get()
                if (raw != null && raw.metaClass?.respondsTo(raw, "isPasscodeStringDetectionEnabled")) {
                    allowStringDetection = raw.isPasscodeStringDetectionEnabled()
                }
            } catch (Exception ignored) {
            }
            if (!allowStringDetection) {
                return false
            }
            String reason = String.valueOf(loginState?.getFailureReason() ?: "").toLowerCase()
            if (!reason) return false
            return reason.contains("passcode")
                    || reason.contains("pin")
                    || reason.contains("otp")
                    || reason.contains("captcha")
                    || reason.contains("security code")
        } catch (Exception ignored) {
            return false
        }
    }

    private long ensureInteractiveWaitDeadline(def ctx, long timeoutMs) {
        def existing = ctx.getPersistentData().get(KEY_INTERACTIVE_WAIT_UNTIL_MS)
        if (existing instanceof Number) return ((Number) existing).longValue()
        long deadline = System.currentTimeMillis() + Math.max(1000L, timeoutMs)
        ctx.getPersistentData().put(KEY_INTERACTIVE_WAIT_UNTIL_MS, deadline)
        return deadline
    }

    private long readInteractiveWaitUntil(def ctx) {
        def existing = ctx.getPersistentData().get(KEY_INTERACTIVE_WAIT_UNTIL_MS)
        return (existing instanceof Number) ? ((Number) existing).longValue() : 0L
    }

    private boolean isInteractiveWaitTimedOut(def ctx) {
        long waitUntil = readInteractiveWaitUntil(ctx)
        return waitUntil > 0L && System.currentTimeMillis() >= waitUntil
    }

    private long nextRetryDelayMs(def ctx, def settings) {
        def attemptRaw = ctx.getPersistentData().getOrDefault("loginRetryAttempt", 0)
        int attempt = (attemptRaw instanceof Number) ? attemptRaw.intValue() : 0
        int nextAttempt = attempt + 1
        ctx.getPersistentData().put("loginRetryAttempt", nextAttempt)

        if (settings == null) {
            int index = Math.max(0, Math.min(nextAttempt - 1, RETRY_DELAYS_MS.size() - 1))
            return clampDelay(RETRY_DELAYS_MS.get(index))
        }

        long baseDelay = Math.max(0L, (long) toInt(settings.retryBaseDelayMs, 5000))
        long maxDelay = Math.max(baseDelay, (long) toInt(settings.retryMaxDelayMs, 60000))
        maxDelay = Math.min(MAX_RETRY_DELAY_MS, maxDelay)

        long cap = baseDelay
        if (nextAttempt > 1) {
            long exp = (1L << Math.min(nextAttempt - 1, 20))
            cap = baseDelay * exp
        }
        cap = Math.min(cap, maxDelay)
        if (cap < 0L) cap = maxDelay // overflow guard

        // AWS-style Full Jitter: random_between(0, cap), then apply system minimums.
        long jittered = (cap <= 0L) ? 0L : java.util.concurrent.ThreadLocalRandom.current().nextLong(0L, cap + 1L)
        long systemMin = MIN_RETRY_DELAY_MS
        return clampDelay(Math.max(systemMin, jittered))
    }

    private boolean canRetry(def ctx, def settings, String failureClass) {
        if (settings == null) return true
        boolean infiniteRequested = toBool(settings.infiniteRetryMode, true)
        if (infiniteRequested && (FAILURE_NETWORK.equals(failureClass)
                || FAILURE_AGENT_TIMEOUT.equals(failureClass)
                || FAILURE_SERVER_INSPECTION.equals(failureClass)
                || FAILURE_GHOST_COOLDOWN.equals(failureClass))) return true
        if (infiniteRequested && !FAILURE_NETWORK.equals(failureClass)
                && !FAILURE_AGENT_TIMEOUT.equals(failureClass)
                && !FAILURE_SERVER_INSPECTION.equals(failureClass)
                && !FAILURE_GHOST_COOLDOWN.equals(failureClass)) return false

        int maxAttempts = toInt(settings.maxRetryAttempts, 0)
        if (maxAttempts <= 0) {
            return true
        }
        def attemptRaw = ctx.getPersistentData().getOrDefault("loginRetryAttempt", 0)
        int attempt = (attemptRaw instanceof Number) ? attemptRaw.intValue() : 0
        return attempt < maxAttempts
    }

    /**
     * @param fetchFromProvider false: return cached runtime map only (may be null).
     *                          true: use cache if present; otherwise load from provider.
     *                          Explicit connect always bypasses cache via buildAndStoreRuntimeSettings.
     */
    private Map resolveRuntimeSettings(def ctx, def settingsProvider, boolean fetchFromProvider) {
        boolean explicitConnect = Boolean.TRUE.equals(ctx.getPersistentData().get("explicitConnectRequested"))
        if (explicitConnect) {
            return buildAndStoreRuntimeSettings(ctx, settingsProvider, true)
        }
        if (!fetchFromProvider) {
            def existing = ctx.getPersistentData().get(KEY_RUNTIME_LOGIN_SETTINGS)
            return existing instanceof Map ? existing : null
        }
        def existing = ctx.getPersistentData().get(KEY_RUNTIME_LOGIN_SETTINGS)
        if (existing instanceof Map) {
            return existing
        }
        return buildAndStoreRuntimeSettings(ctx, settingsProvider, false)
    }

    private Map buildAndStoreRuntimeSettings(def ctx, def settingsProvider, boolean explicitSideEffects) {
        def raw = null
        try {
            raw = settingsProvider?.get()
        } catch (Exception e) {
            log.warn("Login settings get() failed for machine {}: {}", ctx?.getMachineName(), e.getMessage())
        }
        Map snapshot
        if (raw == null) {
            snapshot = emptyRuntimeLoginSettingsSnapshot()
        } else {
            snapshot = [
                    targetGateway    : String.valueOf(raw.getTargetGateway() ?: ""),
                    username         : String.valueOf(raw.getUsername() ?: ""),
                    password         : String.valueOf(raw.getPassword() ?: ""),
                    passcode         : String.valueOf(raw.getPasscode() ?: ""),
                    targetAgent      : String.valueOf(raw.getTargetAgent() ?: ""),
                    selectedCharacter: String.valueOf(raw.getSelectedCharacter() ?: ""),
                    locale           : raw.getLocale(),
                    autoLogin        : raw.isAutoLogin(),
                    autoReconnect    : raw.isAutoReconnect(),
                    retryBaseDelayMs : raw.getRetryBaseDelayMs(),
                    retryMaxDelayMs  : raw.getRetryMaxDelayMs(),
                    maxRetryAttempts : raw.getMaxRetryAttempts(),
                    infiniteRetryMode: raw.isInfiniteRetryMode(),
                    loginCharset     : resolveSettingField(raw, "loginCharset", "windows-1252"),
                    agentWaitTimeoutMs: normalizeTimeout("agentWaitTimeoutMs", resolveSettingField(raw, "agentWaitTimeoutMs", String.valueOf(AGENT_WAIT_TIMEOUT_MS)), AGENT_WAIT_TIMEOUT_MS, 5000L, 30000L),
                    loginResponseTimeoutMs: normalizeTimeout("loginResponseTimeoutMs", resolveSettingField(raw, "loginResponseTimeoutMs", String.valueOf(LOGIN_RESPONSE_TIMEOUT_MS)), LOGIN_RESPONSE_TIMEOUT_MS, 15000L, 30000L),
                    agentAuthTimeoutMs: normalizeTimeout("agentAuthTimeoutMs", resolveSettingField(raw, "agentAuthTimeoutMs", String.valueOf(AGENT_AUTH_TIMEOUT_MS)), AGENT_AUTH_TIMEOUT_MS, 10000L, 30000L),
                    passcodeWaitTimeoutMs: normalizeTimeout("passcodeWaitTimeoutMs", resolveSettingField(raw, "passcodeWaitTimeoutMs", String.valueOf(PASSCODE_WAIT_TIMEOUT_MS)), PASSCODE_WAIT_TIMEOUT_MS, 5000L, 120000L),
                    passcodeUserInputTimeoutMs: normalizeTimeout("passcodeUserInputTimeoutMs", resolveSettingField(raw, "passcodeUserInputTimeoutMs", String.valueOf(PASSCODE_USER_INPUT_TIMEOUT_MS)), PASSCODE_USER_INPUT_TIMEOUT_MS, 5000L, 180000L),
                    agentRequestMaxRetries: Math.max(0, toInt(resolveSettingField(raw, "agentRequestMaxRetries", "2"), 2)),
                    agentRequestRetryBackoffMs: normalizeTimeout("agentRequestRetryBackoffMs", resolveSettingField(raw, "agentRequestRetryBackoffMs", "2000"), 2000L, 500L, 60000L),
                    agentBanBlockDurationMs: normalizeTimeout("agentBanBlockDurationMs", resolveSettingField(raw, "agentBanBlockDurationMs", "300000"), 300000L, 10000L, 3600000L),
                    gatewayLoginMinIntervalMs: normalizeTimeout("gatewayLoginMinIntervalMs", resolveSettingField(raw, "gatewayLoginMinIntervalMs", "2000"), 2000L, 0L, 60000L),
                    gatewayLoginPauseAfterAgentListMs: normalizeTimeout("gatewayLoginPauseAfterAgentListMs", resolveSettingField(raw, "gatewayLoginPauseAfterAgentListMs", "1500"), 1500L, 0L, 60000L),
                    gatewayClientVersion: toInt(resolveSettingField(raw, "gatewayClientVersion", "188"), 188),
                    gatewayClientModule: normalizeGatewayClientModule(resolveSettingField(raw, "gatewayClientModule", "SR_Client")),
                    selectedCharacterSlot: toInt(resolveSettingField(raw, "selectedCharacterSlot", "-1"), -1),
                    characterSlotBase: toInt(resolveSettingField(raw, "characterSlotBase", "0"), 0),
                    characterSelectionStrictMode: toBool(resolveSettingField(raw, "characterSelectionStrictMode", "false"), false),
                    passcodeStringDetectionEnabled: toBool(resolveSettingField(raw, "passcodeStringDetectionEnabled", "true"), true),
                    logoutAckTimeoutMs: normalizeTimeout("logoutAckTimeoutMs", resolveSettingField(raw, "logoutAckTimeoutMs", String.valueOf(LOGOUT_ACK_TIMEOUT_MS)), LOGOUT_ACK_TIMEOUT_MS, 500L, 10000L)
            ]
            enforceTimeoutHierarchy(snapshot)
        }
        ctx.getPersistentData().put(KEY_RUNTIME_LOGIN_SETTINGS, snapshot)
        if (explicitSideEffects) {
            ctx.getPersistentData().remove("uiLoginPhase")
            ctx.getPersistentData().remove(KEY_UI_LOGIN_PHASE_EVENT_DEDUP)
            ctx.getPersistentData().remove(KEY_LOGIN_LAST_HALT_SIGNATURE)
            ctx.getPersistentData().remove(KEY_USER_RESUME_REQUIRED)
            ctx.getPersistentData().remove(KEY_LOGIN_HALTED_CREDENTIAL)
            ctx.getPersistentData().remove(KEY_LAST_GATEWAY_LOGIN_REQUEST_MS)
            try {
                def ls = ctx?.getGameModel()?.getLoginState()
                if (ls != null && ls.getPhase() == LoginState.Phase.FAILED) {
                    ls.setPhase(LoginState.Phase.DISCONNECTED)
                    ls.setFailureReason(null)
                    ls.clearLoginResultCodes()
                }
            } catch (Exception ignored) {
            }
            log.info("Refreshed runtime login settings for explicit connect on machine {}", ctx.getMachineName())
        }
        return snapshot
    }

    private Map emptyRuntimeLoginSettingsSnapshot() {
        Map snap = [
                targetGateway    : "",
                username         : "",
                password         : "",
                passcode         : "",
                targetAgent      : "",
                selectedCharacter: "",
                locale           : null,
                autoLogin        : false,
                autoReconnect    : false,
                retryBaseDelayMs : 5000,
                retryMaxDelayMs  : 60000,
                maxRetryAttempts : 0,
                infiniteRetryMode: true,
                loginCharset     : "windows-1252",
                agentWaitTimeoutMs: AGENT_WAIT_TIMEOUT_MS,
                loginResponseTimeoutMs: LOGIN_RESPONSE_TIMEOUT_MS,
                agentAuthTimeoutMs: AGENT_AUTH_TIMEOUT_MS,
                passcodeWaitTimeoutMs: PASSCODE_WAIT_TIMEOUT_MS,
                passcodeUserInputTimeoutMs: PASSCODE_USER_INPUT_TIMEOUT_MS,
                agentRequestMaxRetries: 2,
                agentRequestRetryBackoffMs: 2000L,
                agentBanBlockDurationMs: 300000L,
                gatewayLoginMinIntervalMs: 2000L,
                gatewayLoginPauseAfterAgentListMs: 1500L,
                gatewayClientVersion: 188,
                gatewayClientModule: "SR_Client",
                selectedCharacterSlot: -1,
                characterSlotBase: 0,
                characterSelectionStrictMode: false,
                passcodeStringDetectionEnabled: true,
                logoutAckTimeoutMs: LOGOUT_ACK_TIMEOUT_MS
        ]
        enforceTimeoutHierarchy(snap)
        return snap
    }

    private boolean isHalted(def ctx, def loginState) {
        if (Boolean.TRUE.equals(ctx?.getPersistentData()?.get(KEY_LOGIN_HALTED_CREDENTIAL))) {
            return true
        }
        try {
            def p = loginState?.getPhase()
            if (p == LoginState.Phase.FAILED) {
                return true
            }
        } catch (Exception ignored) {
        }
        return false
    }

    private String buildHaltSignature(def ctx, def loginState, String failureClass) {
        String machine = String.valueOf(ctx?.getMachineName() ?: "")
        String phase = String.valueOf(loginState?.getPhase() ?: "DISCONNECTED")
        String reason = String.valueOf(loginState?.getFailureReason() ?: "")
        String connected = "false"
        try {
            connected = String.valueOf(ctx?.getDispatcher()?.isServerConnected())
        } catch (Exception ignored) {}
        return machine + "|" + phase + "|" + failureClass + "|" + connected + "|" + reason
    }

    private void beginAttemptSnapshot(def ctx, def settings) {
        if (!(settings instanceof Map)) return
        ctx.getPersistentData().put(KEY_ATTEMPT_LOGIN_SETTINGS, new LinkedHashMap(settings))
        long current = currentAttemptId(ctx)
        ctx.getPersistentData().put(KEY_ACTIVE_ATTEMPT_ID, current + 1L)
    }

    private String resolveSettingField(def settingsObj, String fieldName, String defaultValue) {
        if (settingsObj == null || fieldName == null || fieldName.isEmpty()) return defaultValue
        try {
            def value = settingsObj."${fieldName}"
            if (value == null) return defaultValue
            String asString = String.valueOf(value).trim()
            return asString.isEmpty() ? defaultValue : asString
        } catch (Exception ignored) {
            return defaultValue
        }
    }

    private long normalizeTimeout(String name, def rawValue, long defaultValue, long minValue, long maxValue) {
        long value = toLong(rawValue, defaultValue)
        long clamped = Math.max(minValue, Math.min(maxValue, value))
        if (clamped != value) {
            log.warn("Clamped {} from {} to {}", name, value, clamped)
        }
        return clamped
    }

    private void enforceTimeoutHierarchy(Map snapshot) {
        long parent = (snapshot?.loginResponseTimeoutMs instanceof Number) ? ((Number) snapshot.loginResponseTimeoutMs).longValue() : LOGIN_RESPONSE_TIMEOUT_MS
        long childAgent = (snapshot?.agentWaitTimeoutMs instanceof Number) ? ((Number) snapshot.agentWaitTimeoutMs).longValue() : AGENT_WAIT_TIMEOUT_MS
        if (childAgent >= parent) {
            snapshot.agentWaitTimeoutMs = Math.max(1000L, parent - 1000L)
            log.warn("Adjusted agentWaitTimeoutMs to {} to keep it below loginResponseTimeoutMs={}", snapshot.agentWaitTimeoutMs, parent)
        }
        int retries = (snapshot?.agentRequestMaxRetries instanceof Number) ? ((Number) snapshot.agentRequestMaxRetries).intValue() : 0
        long backoff = (snapshot?.agentRequestRetryBackoffMs instanceof Number) ? ((Number) snapshot.agentRequestRetryBackoffMs).longValue() : 2000L
        long perAttempt = (snapshot?.agentWaitTimeoutMs instanceof Number) ? ((Number) snapshot.agentWaitTimeoutMs).longValue() : AGENT_WAIT_TIMEOUT_MS
        long totalBudget = perAttempt * (retries + 1L) + (backoff * retries)
        if (totalBudget > parent) {
            long divisor = Math.max(1L, perAttempt + backoff)
            long headroom = parent - perAttempt
            // Groovy '/' can yield BigDecimal and break Math.max(long, ?); use integer division.
            long computed = headroom > 0L ? headroom.intdiv(divisor) : 0L
            int maxRetries = (int) Math.max(0L, computed)
            if (maxRetries < retries) {
                snapshot.agentRequestMaxRetries = maxRetries
                log.warn("Adjusted agentRequestMaxRetries from {} to {} to fit loginResponseTimeoutMs budget", retries, maxRetries)
            }
        }
    }

    private long effectiveAgentWaitTimeoutMs(def settings) {
        if (settings?.agentWaitTimeoutMs instanceof Number) return ((Number) settings.agentWaitTimeoutMs).longValue()
        return AGENT_WAIT_TIMEOUT_MS
    }

    private long effectiveLoginResponseTimeoutMs(def settings) {
        if (settings?.loginResponseTimeoutMs instanceof Number) return ((Number) settings.loginResponseTimeoutMs).longValue()
        return LOGIN_RESPONSE_TIMEOUT_MS
    }

    private long effectiveAgentAuthTimeoutMs(def settings) {
        if (settings?.agentAuthTimeoutMs instanceof Number) return ((Number) settings.agentAuthTimeoutMs).longValue()
        return AGENT_AUTH_TIMEOUT_MS
    }

    private long effectivePasscodeUserInputTimeoutMs(def settings) {
        if (settings?.passcodeUserInputTimeoutMs instanceof Number) return ((Number) settings.passcodeUserInputTimeoutMs).longValue()
        return PASSCODE_USER_INPUT_TIMEOUT_MS
    }

    private static final String DISPATCHER_IMPL_CLASS_NAME = 'org.sokybot.engine.core.dispatcher.DispatcherImpl'

    /**
     * Resolves {@code IProxyConnection} for server sends when {@code IDispatcher} / {@code IWorkflowContext}
     * interface dispatch is broken (e.g. OSGi {@code AbstractMethodError}). Uses the concrete dispatcher field last.
     */
    private Object resolveWorkflowProxyConnection(def ctx, def disp) {
        try {
            def p = ctx?.getProxyConnection()
            if (p != null) {
                return p
            }
        } catch (Throwable ignored) {
            // Skewed IWorkflowContext vs runtime class
        }
        if (disp == null) {
            return null
        }
        try {
            def cls = disp.getClass()
            if (DISPATCHER_IMPL_CLASS_NAME == cls.getName()) {
                def f = cls.getDeclaredField('proxyConnection')
                f.setAccessible(true)
                return f.get(disp)
            }
        } catch (Throwable e) {
            log.debug("Could not read proxyConnection from dispatcher: {}", e.getMessage())
        }
        return null
    }

    /**
     * Sends a server-bound workflow packet. When {@code DISPATCHER_DYNAMIC_PACKET_API_OK} is false (e.g. OSGi
     * {@code AbstractMethodError} on {@code IDispatcher}), uses only {@code IProxyConnection} — never the broken dispatcher.
     *
     * @return true if the packet was sent; false if nothing was sent (caller should treat as failure)
     */
    private boolean sendWorkflowServerPacket(def ctx, MutablePacket pkt) {
        def disp = ctx != null ? ctx.getDispatcher() : null
        if (disp == null) {
            try {
                disp = context?.getDispatcher()
            } catch (Exception ignored) {
            }
        }
        if (DISPATCHER_DYNAMIC_PACKET_API_OK.get() == Boolean.FALSE) {
            try {
                def proxy = resolveWorkflowProxyConnection(ctx, disp)
                if (proxy != null) {
                    proxy.sendToServer(pkt)
                    return true
                }
            } catch (Throwable e) {
                log.warn("Proxy sendToServer failed while dispatcher API disabled: {}", e.getMessage())
            }
            log.error("SEVERE: Dispatcher packet API disabled and no proxy; cannot send server packet")
            return false
        }
        if (disp == null) {
            try {
                def proxy = resolveWorkflowProxyConnection(ctx, null)
                if (proxy != null) {
                    proxy.sendToServer(pkt)
                    return true
                }
            } catch (Throwable e) {
                log.warn("Proxy sendToServer failed (no dispatcher): {}", e.getMessage())
            }
            log.error("SEVERE: No dispatcher and no proxy; cannot send server packet")
            return false
        }
        try {
            disp.sendToServer(pkt)
            return true
        } catch (Throwable t) {
            if (isDispatcherPacketApiChainFailure(t)) {
                DISPATCHER_DYNAMIC_PACKET_API_OK.set(Boolean.FALSE)
                logDispatcherPacketApiFirstFailure(disp, t)
                try {
                    def proxy = resolveWorkflowProxyConnection(ctx, disp)
                    if (proxy != null) {
                        proxy.sendToServer(pkt)
                        return true
                    }
                } catch (Throwable e) {
                    log.warn("Proxy fallback after dispatcher failure: {}", e.getMessage())
                }
                log.error("SEVERE: Dispatcher failed and proxy unavailable; cannot send server packet")
                return false
            }
            if (t instanceof Error && !(t instanceof Exception)) {
                throw (Error) t
            }
            if (t instanceof Exception) {
                throw (Exception) t
            }
            throw new RuntimeException(t)
        }
    }

    private static boolean isDispatcherPacketApiChainFailure(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof MissingMethodException) return true
            if (c instanceof AbstractMethodError) return true
            if (c instanceof NoSuchMethodError) return true
            if (c instanceof IncompatibleClassChangeError) return true
        }
        return false
    }

    private void logDispatcherPacketApiFirstFailure(def disp, Throwable t) {
        if (LOGGED_DISPATCHER_PACKET_API_FAILURE.compareAndSet(false, true)) {
            String cls = disp != null ? String.valueOf(disp.getClass().getName()) : "null"
            String head = "First failure detected; using raw packet fallbacks until JVM restart or script reload (receiver=" + cls + ")"
            if (t != null) {
                log.error(head + ": " + t.getMessage(), t)
            } else {
                log.error(head)
            }
        }
    }

    /**
     * 0x6101 via sendToServer with DispatcherImpl-equivalent spacing; state in workflow persistent data (per machine).
     */
    private boolean sendAgentRequestFallback(def ctx, def settings, boolean allowBypass) {
        long now = System.currentTimeMillis()
        def pd = ctx?.getPersistentData()
        if (pd == null) {
            log.error("SEVERE: Cannot enforce agent-request rate limit (no persistent data); skipping 0x6101 fallback send")
            return false
        }
        long last = (pd.get(KEY_FALLBACK_AGENT_REQUEST_LAST_MS) instanceof Number)
                ? ((Number) pd.get(KEY_FALLBACK_AGENT_REQUEST_LAST_MS)).longValue() : 0L
        boolean bypass = allowBypass && !Boolean.TRUE.equals(pd.get(KEY_FALLBACK_AGENT_COLD_BYPASS_USED))
        if (!bypass && last > 0L && (now - last) < AGENT_REQUEST_MIN_INTERVAL_MS) {
            long left = AGENT_REQUEST_MIN_INTERVAL_MS - (now - last)
            log.error("SEVERE: Skipping agent list request (fallback rate limit); dispatcher packet API unavailable, {} ms left in interval", left)
            return false
        }
        MutablePacket agentPkt = MutablePacket.getBuilder(0, ClientOpcode.AGENT_REQUEST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .build()
        if (!sendWorkflowServerPacket(ctx, agentPkt)) {
            return false
        }
        pd.put(KEY_FALLBACK_AGENT_REQUEST_LAST_MS, now)
        if (bypass) {
            pd.put(KEY_FALLBACK_AGENT_COLD_BYPASS_USED, Boolean.TRUE)
        }
        return true
    }

    private boolean requestAgentList(def ctx, def settings, boolean allowBypass) {
        long now = System.currentTimeMillis()
        long cacheAt = (ctx.getPersistentData().get(KEY_AGENT_LIST_CACHE_AT_MS) instanceof Number)
                ? ((Number) ctx.getPersistentData().get(KEY_AGENT_LIST_CACHE_AT_MS)).longValue() : 0L
        def s = snapshot(ctx)
        if (cacheAt > 0L && (now - cacheAt) <= 300000L && Boolean.TRUE.equals(s.agentListReceived)) {
            log.info("Using cached agent list (age={}ms), skipping 0x6101", now - cacheAt)
            return true
        }
        long banUntil = (ctx.getPersistentData().get("agentBanUntilMs") instanceof Number)
                ? ((Number) ctx.getPersistentData().get("agentBanUntilMs")).longValue() : 0L
        if (banUntil > now) {
            log.warn("Agent request blocked by ban window for {} ms", banUntil - now)
            return false
        }
        try {
            def disp = ctx?.getDispatcher()
            if (disp == null) {
                MutablePacket agentPkt = MutablePacket.getBuilder(0, ClientOpcode.AGENT_REQUEST)
                        .packetEncoding(Encoding.ENCRYPTED)
                        .dataEncoding(Encoding.PLAIN)
                        .packetSource(NetworkPeer.BOT)
                        .build()
                try {
                    def proxy = resolveWorkflowProxyConnection(ctx, null)
                    if (proxy != null) {
                        proxy.sendToServer(agentPkt)
                    } else {
                        sendToServer(0, ClientOpcode.AGENT_REQUEST) { it }
                    }
                } catch (Exception e) {
                    log.warn("Agent request (no dispatcher): {}", e.getMessage())
                    sendToServer(0, ClientOpcode.AGENT_REQUEST) { it }
                }
                return true
            }
            Boolean pathOk = DISPATCHER_DYNAMIC_PACKET_API_OK.get()
            if (pathOk != Boolean.FALSE) {
                try {
                    def r = disp.sendAgentRequest(allowBypass)
                    DISPATCHER_DYNAMIC_PACKET_API_OK.compareAndSet(null, Boolean.TRUE)
                    return r as boolean
                } catch (RateLimitException e) {
                    log.warn("Agent request rate-limited: {}", e.getMessage())
                    return false
                } catch (Throwable t) {
                    if (isDispatcherPacketApiChainFailure(t)) {
                        DISPATCHER_DYNAMIC_PACKET_API_OK.set(Boolean.FALSE)
                        logDispatcherPacketApiFirstFailure(disp, t)
                        return sendAgentRequestFallback(ctx, settings, allowBypass)
                    }
                    if (t instanceof Error && !(t instanceof Exception)) {
                        throw (Error) t
                    }
                    if (t instanceof Exception) {
                        throw (Exception) t
                    }
                    throw new RuntimeException(t)
                }
            }
            return sendAgentRequestFallback(ctx, settings, allowBypass)
        } catch (RateLimitException e) {
            log.warn("Agent request rate-limited: {}", e.getMessage())
            return false
        } catch (Exception e) {
            long cooldown = (settings?.agentRequestRetryBackoffMs instanceof Number) ? ((Number) settings.agentRequestRetryBackoffMs).longValue() : 2000L
            ctx.getPersistentData().put(KEY_AGENT_BYPASS_COOLDOWN_UNTIL_MS, now + cooldown)
            log.warn("Agent request failed: {}", e.getMessage())
            return false
        }
    }

    private void sendLoginRequest(def ctx, byte locale, String username, String password, short agentId, String charsetName) {
        if ((locale & 0xFF) == 0) {
            locale = (byte) 22
        }
        boolean legacy = Boolean.parseBoolean(System.getProperty("sokybot.login.serializer.legacy", "false"))
        def disp = ctx?.getDispatcher()
        if (!legacy && disp != null && DISPATCHER_DYNAMIC_PACKET_API_OK.get() != Boolean.FALSE) {
            try {
                disp.sendLoginRequest(locale, username, password, ((int) agentId) & 0xFFFF, charsetName)
                DISPATCHER_DYNAMIC_PACKET_API_OK.compareAndSet(null, Boolean.TRUE)
                return
            } catch (Throwable t) {
                if (isDispatcherPacketApiChainFailure(t)) {
                    DISPATCHER_DYNAMIC_PACKET_API_OK.set(Boolean.FALSE)
                    logDispatcherPacketApiFirstFailure(disp, t)
                } else {
                    if (t instanceof Error && !(t instanceof Exception)) throw (Error) t
                    if (t instanceof Exception) throw (Exception) t
                    throw new RuntimeException(t)
                }
            }
        }
        try {
            disp?.awaitGatewayLoginPauseAfterAgentList()
        } catch (MissingMethodException ignored) {
        }
        Charset charset = resolveLoginCharset([loginCharset: charsetName])
        byte[] usernameBytes = username.getBytes(charset)
        byte[] passwordBytes = password.getBytes(charset)
        int packetLen = 7 + usernameBytes.length + passwordBytes.length
        MutablePacket loginPkt = MutablePacket.getBuilder(packetLen, ClientOpcode.LOGIN_REQUEST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(locale)
                .putShort((short) usernameBytes.length)
                .putBytes(usernameBytes)
                .putShort((short) passwordBytes.length)
                .putBytes(passwordBytes)
                .putShort(agentId)
                .build()
        if (!sendWorkflowServerPacket(ctx, loginPkt)) {
            throw new IllegalStateException("Failed to send login request packet (dispatcher/proxy unavailable)")
        }
    }

    private void sendGatewayImageCodeAnswer(def ctx, String answer) {
        String a = answer != null ? answer : ""
        byte[] utf16 = a.getBytes(StandardCharsets.UTF_16LE)
        if ((utf16.length & 1) != 0) {
            throw new IllegalStateException("UTF-16LE byte length must be even")
        }
        int wcharCount = utf16.length / 2
        if (wcharCount > 65535) {
            throw new IllegalStateException("Image code answer too long")
        }
        int packetLen = 2 + utf16.length
        MutablePacket pkt = MutablePacket.getBuilder(packetLen, ClientOpcode.GATEWAY_IMAGE_CODE_ANSWER)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .putShort((short) wcharCount)
                .putBytes(utf16)
                .build()
        if (!sendWorkflowServerPacket(ctx, pkt)) {
            throw new IllegalStateException("Failed to send gateway 0x6323")
        }
    }


    private Map currentAttemptSettings(def ctx, def settingsProvider) {
        def existing = ctx.getPersistentData().get(KEY_ATTEMPT_LOGIN_SETTINGS)
        if (existing instanceof Map) {
            return existing
        }
        def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
        if (settings instanceof Map) {
            beginAttemptSnapshot(ctx, settings)
        }
        return settings
    }

    private long currentAttemptId(def ctx) {
        def raw = ctx.getPersistentData().get(KEY_ACTIVE_ATTEMPT_ID)
        if (raw instanceof Number) return ((Number) raw).longValue()
        return 0L
    }

    private void safeDisconnect(def sourceCtx) {
        try {
            def dispatcher = null
            try { dispatcher = sourceCtx?.getDispatcher() } catch (Exception ignored) {}
            if (dispatcher == null) {
                try { dispatcher = sourceCtx?.getProxyConnection() } catch (Exception ignored) {}
            }
            if (dispatcher == null) return
            if (dispatcher.metaClass?.respondsTo(dispatcher, "disconnect")) {
                dispatcher.disconnect()
                return
            }
            if (dispatcher.metaClass?.respondsTo(dispatcher, "close")) {
                dispatcher.close()
            }
        } catch (Exception e) {
            logErrorThrottled(sourceCtx, "LOGIN_SAFE_DISCONNECT_01", e)
        }
    }

    private boolean isAgentSessionEstablished(def ctx) {
        try {
            def ls = ctx?.getGameModel()?.getLoginState()
            def phase = ls?.getPhase()
            boolean phaseReady = phase == LoginState.Phase.AUTHENTICATED
                    || phase == LoginState.Phase.LOADING_ENVIRONMENT
                    || phase == LoginState.Phase.IN_GAME
            boolean socketOpen = ctx?.getDispatcher() != null && ctx.getDispatcher().isServerConnected()
            return phaseReady && socketOpen
        } catch (Exception ignored) {
            return false
        }
    }

    private void gracefulShutdown(def ctx) {
        try {
            if (isAgentSessionEstablished(ctx)) {
                try {
                    def d = ctx?.getDispatcher()
                    if (d != null && DISPATCHER_DYNAMIC_PACKET_API_OK.get() != Boolean.FALSE) {
                        try {
                            d.sendLogoutRequest()
                            DISPATCHER_DYNAMIC_PACKET_API_OK.compareAndSet(null, Boolean.TRUE)
                        } catch (Throwable t) {
                            if (isDispatcherPacketApiChainFailure(t)) {
                                DISPATCHER_DYNAMIC_PACKET_API_OK.set(Boolean.FALSE)
                                logDispatcherPacketApiFirstFailure(d, t)
                                MutablePacket lo = MutablePacket.getBuilder(0, ClientOpcode.LOGOUT_REQUEST)
                                        .packetEncoding(Encoding.ENCRYPTED)
                                        .dataEncoding(Encoding.PLAIN)
                                        .packetSource(NetworkPeer.BOT)
                                        .build()
                                sendWorkflowServerPacket(ctx, lo)
                            } else {
                                log.warn("Logout packet send failed during shutdown: {}", t.getMessage())
                            }
                        }
                    } else if (d != null) {
                        MutablePacket lo = MutablePacket.getBuilder(0, ClientOpcode.LOGOUT_REQUEST)
                                .packetEncoding(Encoding.ENCRYPTED)
                                .dataEncoding(Encoding.PLAIN)
                                .packetSource(NetworkPeer.BOT)
                                .build()
                        sendWorkflowServerPacket(ctx, lo)
                    }
                } catch (Exception e) {
                    log.warn("Logout packet send failed during shutdown: {}", e.getMessage())
                }
                long timeout = LOGOUT_ACK_TIMEOUT_MS
                try {
                    def settings = resolveRuntimeSettings(ctx, loginSettingsProvider, false)
                    if (settings?.logoutAckTimeoutMs instanceof Number) timeout = ((Number) settings.logoutAckTimeoutMs).longValue()
                } catch (Exception ignored) {}
                long until = System.currentTimeMillis() + Math.max(500L, timeout)
                while (System.currentTimeMillis() < until) {
                    try {
                        if (ctx?.getDispatcher() == null || !ctx.getDispatcher().isServerConnected()) break
                    } catch (Exception ignored) {
                        break
                    }
                    try { Thread.sleep(100L) } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break }
                }
            }
        } finally {
            safeDisconnect(ctx)
        }
    }

    private Map snapshot(def ctx) {
        def loginState = null
        def phase = null
        def failureReason = null
        boolean agentListReceived = false
        int agentListCount = 0
        try {
            loginState = ctx?.getGameModel()?.getLoginState()
            phase = loginState?.getPhase()
            failureReason = loginState?.getFailureReason()
            def agents = loginState?.getAgentList()
            agentListCount = (agents instanceof Collection) ? agents.size() : 0
            agentListReceived = (phase == LoginState.Phase.AGENTS_RECEIVED) || agentListCount > 0
        } catch (Exception e) {
            logErrorThrottled(ctx, "LOGIN_SNAPSHOT_02", e)
        }
        boolean serverConnected = false
        try {
            serverConnected = ctx?.getDispatcher() != null && ctx.getDispatcher().isServerConnected()
        } catch (Exception ignored) {
        }
        boolean loggedIn = isLoggedIn(ctx)
        long nowMs = System.currentTimeMillis()
        return [
                loginState    : loginState,
                phase         : phase,
                failureReason : failureReason,
                agentListReceived: agentListReceived,
                agentListCount: agentListCount,
                serverConnected: serverConnected,
                loggedIn      : loggedIn,
                nowMs         : nowMs
        ]
    }

    private Map readLatestNetworkFailure(def ctx) {
        try {
            def raw = ctx?.getPersistentData()?.get(KEY_NETWORK_TRANSITIONS)
            if (!(raw instanceof List)) return null
            // Scan newest-first for a failure-like transition.
            for (int i = raw.size() - 1; i >= 0; i--) {
                def ev = raw.get(i)
                if (!(ev instanceof Map)) continue
                String transition = String.valueOf(ev.transition ?: "")
                if ("Disconnected".equalsIgnoreCase(transition) || "AUTH_FAILED".equalsIgnoreCase(String.valueOf(ev.loginPhase ?: ""))) {
                    return ev
                }
            }
        } catch (Exception ignored) {
        }
        return null
    }

    private boolean handleInteractivePasscodeWait(def ctx, def loginState, def snap, def settings) {
        if (!isPasscodeInputRequired(loginState)) {
            ctx.getPersistentData().remove(KEY_INTERACTIVE_WAIT_UNTIL_MS)
            return false
        }
        if (loginState?.getPhase() == LoginState.Phase.PASSCODE_SUBMITTED && !isInteractiveWaitTimedOut(ctx)) {
            emitEnginePhase(ctx, PHASE_PASSCODE_SUBMITTED, "PasscodeSubmitted", String.valueOf(snap?.failureReason ?: ""))
            return true
        }
        // Abort interactive wait if the socket is already down; re-prompting is pointless.
        try {
            if (ctx?.getDispatcher() != null && !ctx.getDispatcher().isServerConnected()) {
                if (loginState != null) {
                    loginState.setFailureReason("Connection dropped while waiting for passcode")
                }
                ctx.getPersistentData().remove(KEY_INTERACTIVE_WAIT_UNTIL_MS)
                return false
            }
        } catch (Exception ignored) {
        }
        if (!isInteractiveWaitTimedOut(ctx)) {
            long waitUntil = ensureInteractiveWaitDeadline(ctx, effectivePasscodeUserInputTimeoutMs(settings))
            String phaseName = (loginState?.getPhase() == LoginState.Phase.WAIT_FOR_CAPTCHA) ? PHASE_WAIT_FOR_CAPTCHA : PHASE_WAITING_FOR_PASSCODE
            emitEnginePhase(ctx, phaseName, "WaitingForPasscode",
                    String.valueOf(snap?.failureReason ?: ""),
                    [interactive: true, waitUntil: waitUntil])
            return true
        }
        if (loginState != null) {
            loginState.setFailureReason("Passcode/Captcha input timeout")
        }
        ctx.getPersistentData().remove(KEY_INTERACTIVE_WAIT_UNTIL_MS)
        safeDisconnect(ctx)
        return false
    }

    private boolean handleQueueWait(def ctx, def loginState) {
        if (!isInQueueState(loginState)) {
            return false
        }
        boolean connected = false
        try { connected = ctx?.getDispatcher() != null && ctx.getDispatcher().isServerConnected() } catch (Exception ignored) {}
        if (!connected) {
            if (loginState != null) {
                loginState.setFailureReason("Queue connection dropped")
            }
            return false
        }
        emitEnginePhase(ctx, PHASE_IN_QUEUE, "QueueWaiting", String.valueOf(loginState?.getFailureReason() ?: ""),
                [queuePosition: parseQueuePosition(loginState)])
        return true
    }

    private boolean handleMissingPrereq(def ctx, def loginState) {
        if (!isMissingPrereqPhase(loginState)) {
            return false
        }
        try {
            if (ctx?.getDispatcher() != null && !ctx.getDispatcher().isServerConnected()) {
                if (loginState != null) {
                    loginState.setPhase(LoginState.Phase.DISCONNECTED)
                }
            }
        } catch (Exception ignored) {
        }
        ctx.getPersistentData().put(KEY_USER_RESUME_REQUIRED, true)
        ctx.getPersistentData().remove(KEY_RETRY_UNTIL_MS)
        ctx.getPersistentData().remove(KEY_RETRY_ATTEMPT_ID)
        return true
    }

    private boolean handleTerminalFailure(def ctx, def loginState, String failureClass, def settings) {
        if (FAILURE_MISSING_PREREQ.equals(failureClass)) {
            ctx.getPersistentData().put(KEY_USER_RESUME_REQUIRED, true)
            return true
        }
        String haltSignature = buildHaltSignature(ctx, loginState, failureClass)
        String lastHaltSignature = String.valueOf(ctx.getPersistentData().getOrDefault(KEY_LOGIN_LAST_HALT_SIGNATURE, ""))
        boolean haltAlreadyLogged = haltSignature == lastHaltSignature

        if (FAILURE_MANUAL_VERIFICATION.equals(failureClass)) {
            emitEnginePhase(ctx, "MANUAL_VERIFICATION_REQUIRED", "ManualVerification", String.valueOf(loginState?.getFailureReason() ?: ""),
                    [failureClass: failureClass, fatal: true])
            if (!haltAlreadyLogged) {
                log.warn("Login paused: manual verification required ({})", loginState?.getFailureReason())
                ctx.getPersistentData().put(KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature)
            }
            return true
        }
        if (FAILURE_CREDENTIAL.equals(failureClass)) {
            ctx.getPersistentData().put(KEY_LOGIN_HALTED_CREDENTIAL, true)
            String credDetail = credentialFailureDetail(loginState)
            emitEnginePhase(ctx, LoginState.Phase.FAILED.name(), "CredentialFailure", credDetail,
                    [failureClass: failureClass, fatal: true])
            if (!haltAlreadyLogged) {
                log.warn("Infinite retry disabled for credential failures")
                log.error("Login halted: credential failure ({})", credDetail.isEmpty() ? "unknown" : credDetail)
                ctx.getPersistentData().put(KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature)
            }
            return true
        }
        if (FAILURE_CHARACTER_NOT_FOUND.equals(failureClass)) {
            emitEnginePhase(ctx, LoginState.Phase.FAILED.name(), "CharacterUnavailable",
                    String.valueOf(loginState?.getFailureReason() ?: "CHARACTER_SELECTION_UNAVAILABLE"),
                    [failureClass: failureClass, fatal: true])
            return true
        }
        if (FAILURE_AGENT_TIMEOUT.equals(failureClass)) {
            emitEnginePhase(ctx, PHASE_WAITING_FOR_AGENTS_TIMEOUT, "WaitingForAgentsTimeout",
                    String.valueOf(loginState?.getFailureReason() ?: "Agent list timeout"))
        }
        if (FAILURE_SERVER_INSPECTION.equals(failureClass)) {
            long now = System.currentTimeMillis()
            long delayMs = Math.max(nextRetryDelayMs(ctx, settings), 60000L)
            safeDisconnect(ctx)
            emitEnginePhase(ctx, PHASE_SERVER_INSPECTION, "ServerInspection",
                    String.valueOf(loginState?.getFailureReason() ?: "Target server is under inspection."),
                    [retryDelayMs: delayMs, serverTimestamp: now, retryAt: now + delayMs, failureClass: failureClass])
            ctx.getPersistentData().put(KEY_RETRY_UNTIL_MS, now + delayMs)
            ctx.getPersistentData().put(KEY_RETRY_ATTEMPT_ID, currentAttemptId(ctx))
            return true
        }
        if (settings != null && !toBool(settings.autoReconnect, true)) {
            emitEnginePhase(ctx, "RETRY_DISABLED", "RetryDisabled", null,
                    [failureClass: failureClass, fatal: true])
            if (!haltAlreadyLogged) {
                log.warn("Login retries halted: auto reconnect disabled in settings")
                ctx.getPersistentData().put(KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature)
            }
            return true
        }

        if (settings != null && !canRetry(ctx, settings, failureClass)) {
            emitEnginePhase(ctx, "RETRY_LIMIT_REACHED", "RetryLimitReached", null,
                    [failureClass: failureClass, fatal: true])
            if (!haltAlreadyLogged) {
                log.warn("Login retries halted: max retry attempts reached ({})", toInt(settings.maxRetryAttempts, 0))
                ctx.getPersistentData().put(KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature)
            }
            return true
        }
        ctx.getPersistentData().remove(KEY_LOGIN_LAST_HALT_SIGNATURE)
        return false
    }

    private void scheduleRetry(def ctx, def loginState, def snap, String failureClass, def settings) {
        markGatewayAttemptFailure(ctx)
        long requestedDelayMs = settings != null
                ? Math.max(0L, (long) toInt(settings.retryBaseDelayMs, 5000))
                : RETRY_DELAYS_MS.get(0)
        long delayMs = nextRetryDelayMs(ctx, settings)
        if (FAILURE_GHOST_COOLDOWN.equals(failureClass)) {
            delayMs = Math.max(delayMs, 60000L)
        }
        boolean infiniteRequested = settings != null && toBool(settings.infiniteRetryMode, true)
        boolean infiniteEffective = (FAILURE_NETWORK.equals(failureClass) || FAILURE_AGENT_TIMEOUT.equals(failureClass)) && infiniteRequested
        String phase = String.valueOf(snap?.phase ?: "DISCONNECTED")
        String host = String.valueOf(ctx.getPersistentData().get("gatewayHost") ?: "")
        String port = String.valueOf(ctx.getPersistentData().get("gatewayPort") ?: "")
        log.info("Retry policy applied: requestedDelay={} effectiveDelay={} infinite={}",
                requestedDelayMs, delayMs, infiniteEffective)
        log.info("RETRY_DELAY context machine={} phase={} serverConnected={} gateway={}:{}",
                ctx.getMachineName(), phase, ctx.getDispatcher().isServerConnected(), host, port)
        log.warn("Login retry in {} ms (reason: {})", delayMs, loginState?.getFailureReason() ?: "unknown")
        long nowMs = System.currentTimeMillis()
        emitEnginePhase(ctx, "RETRY_DELAY", "RetryDelay", String.valueOf(loginState?.getFailureReason() ?: ""),
                [retryDelayMs: delayMs, serverTimestamp: nowMs, retryAt: nowMs + delayMs, failureClass: failureClass])
        ctx.getPersistentData().put(KEY_RETRY_UNTIL_MS, nowMs + delayMs)
        ctx.getPersistentData().put(KEY_RETRY_ATTEMPT_ID, currentAttemptId(ctx))
    }

    private boolean toBool(def value, boolean defaultValue) {
        if (value == null) return defaultValue
        if (value instanceof Boolean) return ((Boolean) value).booleanValue()
        return String.valueOf(value).equalsIgnoreCase("true")
    }

    private int toInt(def value, int defaultValue) {
        if (value == null) return defaultValue
        if (value instanceof Number) return ((Number) value).intValue()
        try {
            return Integer.parseInt(String.valueOf(value))
        } catch (Exception ignored) {
            return defaultValue
        }
    }

    private long toLong(def value, long defaultValue) {
        if (value == null) return defaultValue
        if (value instanceof Number) return ((Number) value).longValue()
        try {
            return Long.parseLong(String.valueOf(value))
        } catch (Exception ignored) {
            return defaultValue
        }
    }

    private long clampDelay(long value) {
        return Math.max(MIN_RETRY_DELAY_MS, Math.min(value, MAX_RETRY_DELAY_MS))
    }

    private Charset resolveLoginCharset(def settings) {
        String configured = String.valueOf(settings?.loginCharset ?: "").trim()
        if (configured.isEmpty()) {
            return Charset.forName("windows-1252")
        }
        try {
            return Charset.forName(configured)
        } catch (Exception ignored) {
            log.warn("Unsupported login charset '{}', falling back to windows-1252", configured)
            return Charset.forName("windows-1252")
        }
    }

    private String redactForLog(String username) {
        if (username == null || username.isEmpty()) return "<empty>"
        if (username.length() == 1) return "*"
        if (username.length() == 2) return username.charAt(0) + "*"
        return username.substring(0, 1) + "***" + username.substring(username.length() - 1)
    }

    private String classifyFailure(def loginState, def ctx) {
        if (isMissingPrereqPhase(loginState)) {
            return FAILURE_MISSING_PREREQ
        }
        if (isServerInspectionState(loginState)) {
            return FAILURE_SERVER_INSPECTION
        }
        if (isAgentWaitTimedOut(ctx)) {
            return FAILURE_AGENT_TIMEOUT
        }
        if (requiresManualVerification(loginState)) {
            return FAILURE_MANUAL_VERIFICATION
        }
        if (isInQueueState(loginState)) {
            return FAILURE_NETWORK
        }

        try {
            def phase = loginState?.getPhase()
            if ((phase == LoginState.Phase.LOGIN_SUCCESS || phase == LoginState.Phase.REDIRECTING
                    || phase == LoginState.Phase.AGENT_CONNECTED)
                    && ctx?.getDispatcher() != null && !ctx.getDispatcher().isServerConnected()) {
                return FAILURE_NETWORK
            }
        } catch (Exception ignored) {
        }

        String reason = String.valueOf(loginState?.getFailureReason() ?: "").toLowerCase()
        Integer gatewayCode = readGatewayFailureCode(loginState)
        if (gatewayCode != null) {
            if (gatewayCode == GATEWAY_ERR_ALREADY_CONNECTED) {
                return FAILURE_GHOST_COOLDOWN
            }
            if (gatewayCode == GATEWAY_ERR_DISCONNECTED_OR_BLOCKED) {
                return FAILURE_NETWORK
            }
            if (gatewayCode == GATEWAY_ERR_INVALID_CREDENTIAL_1
                    || gatewayCode == GATEWAY_ERR_INVALID_CREDENTIAL_2
                    || gatewayCode == GATEWAY_ERR_INVALID_CREDENTIAL_0B
                    || gatewayCode == GATEWAY_ERR_INVALID_CREDENTIAL_0C
                    || gatewayCode == GATEWAY_ERR_INVALID_CREDENTIAL_0D) {
                return FAILURE_CREDENTIAL
            }
            log.warn("Unhandled gateway login failure code {} (reason='{}')", gatewayCode, String.valueOf(loginState?.getFailureReason() ?: ""))
            return FAILURE_UNKNOWN_RETRY
        }
        if (reason.contains("password")
                || reason.contains("invalid account")
                || reason.contains("invalid credentials")
                || reason.contains("auth failed")
                || reason.contains("rejected")
                || reason.contains("denied")) {
            return FAILURE_CREDENTIAL
        }
        if (reason.contains("already connected")) {
            return FAILURE_GHOST_COOLDOWN
        }
        if (reason.contains("character not found") || reason.contains("character_selection_unavailable")) {
            return FAILURE_CHARACTER_NOT_FOUND
        }
        if (reason.contains("inspection") || reason.contains("maintenance")) {
            return FAILURE_SERVER_INSPECTION
        }
        if (reason.contains("too many requests") || reason.contains("6110")) {
            try {
                def settings = resolveRuntimeSettings(ctx, loginSettingsProvider, false)
                long blockMs = (settings?.agentBanBlockDurationMs instanceof Number) ? ((Number) settings.agentBanBlockDurationMs).longValue() : 300000L
                ctx?.getPersistentData()?.put("agentBanUntilMs", System.currentTimeMillis() + blockMs)
            } catch (Exception ignored) {}
            return FAILURE_NETWORK
        }

        try {
            if (ctx?.getDispatcher() != null && !ctx.getDispatcher().isServerConnected()) {
                return FAILURE_NETWORK
            }
        } catch (Exception ignored) {}

        if (reason.contains("timeout")
                || reason.contains("disconnect")
                || reason.contains("unreachable")
                || reason.contains("refused")
                || reason.contains("network")) {
            return FAILURE_NETWORK
        }

        return FAILURE_UNKNOWN_RETRY
    }

    private Integer readGatewayFailureCode(def loginState) {
        try {
            if (loginState != null && loginState.metaClass?.respondsTo(loginState, "getGatewayResultCode")) {
                def value = loginState.getGatewayResultCode()
                if (value instanceof Number) {
                    return ((Number) value).intValue()
                }
                return null
            }
        } catch (Exception ignored) {
        }
        int parsed = parseGatewayFailureCode(loginState)
        return parsed >= 0 ? parsed : null
    }

    /** Failure reason text, or gateway byte code when reason was cleared but code remains. */
    private String credentialFailureDetail(def loginState) {
        try {
            String r = loginState?.getFailureReason()
            if (r != null && !r.trim().isEmpty()) {
                return r
            }
        } catch (Exception ignored) {
        }
        try {
            Integer code = readGatewayFailureCode(loginState)
            if (code != null) {
                return "gateway code " + (code.intValue() & 0xFF)
            }
        } catch (Exception ignored) {
        }
        return ""
    }

    private boolean isMissingPrereqPhase(def loginState) {
        try {
            def phase = loginState?.getPhase()
            return phase == LoginState.Phase.MISSING_CREDENTIALS || phase == LoginState.Phase.MISSING_AGENT_SERVER
        } catch (Exception ignored) {
            return false
        }
    }

    private boolean isServerInspectionState(def loginState) {
        try {
            def phase = loginState?.getPhase()
            if ("SERVER_INSPECTION".equals(String.valueOf(phase))) return true
            String reason = String.valueOf(loginState?.getFailureReason() ?: "").toLowerCase()
            return reason.contains("inspection")
        } catch (Exception ignored) {
            return false
        }
    }

    private int parseGatewayFailureCode(def loginState) {
        try {
            String reason = String.valueOf(loginState?.getFailureReason() ?: "")
            def m = (reason =~ /(?i)\bcode\s+(\d+)\b/)
            if (m.find()) {
                return Integer.parseInt(String.valueOf(m.group(1)))
            }
        } catch (Exception ignored) {
        }
        return -1
    }

    private boolean isInQueueState(def loginState) {
        try {
            if (loginState?.getPhase() == LoginState.Phase.IN_QUEUE) {
                return true
            }
            String reason = String.valueOf(loginState?.getFailureReason() ?: "").toLowerCase()
            return reason.contains("queue")
        } catch (Exception ignored) {
            return false
        }
    }

    private int parseQueuePosition(def loginState) {
        try {
            if (loginState != null && loginState.metaClass?.respondsTo(loginState, "getQueuePosition")) {
                def value = loginState.getQueuePosition()
                if (value instanceof Number) return ((Number) value).intValue()
            }
            String reason = String.valueOf(loginState?.getFailureReason() ?: "")
            def m = (reason =~ /(?i)\bposition\s+(\d+)\b/)
            if (m.find()) {
                return Integer.parseInt(String.valueOf(m.group(1)))
            }
        } catch (Exception ignored) {
        }
        return -1
    }

    private void resetRetry(def ctx) {
        ctx.getPersistentData().put("loginRetryAttempt", 0)
        ctx.getPersistentData().put(KEY_AGENT_WAIT_TIMED_OUT, false)
        ctx.getPersistentData().remove(KEY_RETRY_UNTIL_MS)
        ctx.getPersistentData().remove(KEY_RETRY_ATTEMPT_ID)
    }

    private boolean isAgentWaitTimedOut(def ctx) {
        try {
            def deadlineRaw = ctx.getPersistentData().get(KEY_AGENT_WAIT_DEADLINE_MS)
            if (!(deadlineRaw instanceof Number)) {
                return false
            }
            long deadlineMs = ((Number) deadlineRaw).longValue()
            return deadlineMs > 0L && System.currentTimeMillis() >= deadlineMs
        } catch (Exception ignored) {
            return false
        }
    }

    private boolean isLoginResponseTimedOut(def ctx) {
        try {
            def deadlineRaw = ctx.getPersistentData().get(KEY_LOGIN_RESPONSE_DEADLINE_MS)
            if (!(deadlineRaw instanceof Number)) {
                return false
            }
            long deadlineMs = ((Number) deadlineRaw).longValue()
            return deadlineMs > 0L && System.currentTimeMillis() >= deadlineMs
        } catch (Exception ignored) {
            return false
        }
    }

    private boolean isAgentAuthTimedOut(def ctx) {
        try {
            def deadlineRaw = ctx.getPersistentData().get(KEY_AGENT_AUTH_DEADLINE_MS)
            if (!(deadlineRaw instanceof Number)) {
                return false
            }
            long deadlineMs = ((Number) deadlineRaw).longValue()
            return deadlineMs > 0L && System.currentTimeMillis() >= deadlineMs
        } catch (Exception ignored) {
            return false
        }
    }

    private boolean isBlank(def value) {
        return value == null || String.valueOf(value).trim().isEmpty()
    }

    private void emitEnginePhase(def ctx, String phase, String transition, String reason, Map<String, Object> extras = null) {
        try {
            def state = ctx?.getGameModel()?.getLoginState()
            if (state != null && reason != null && !reason.isEmpty()) {
                state.setFailureReason(reason)
            }
            LoginState.Phase desired = resolveLoginPhaseEnum(phase)
            if (desired == null || state == null || state.getPhase() != desired) {
                setLoginStatePhase(state, phase)
            }
            boolean legacyEmit = Boolean.parseBoolean(System.getProperty("sokybot.engine.phase.legacy", "true"))
            if (!legacyEmit) {
                return
            }
            def eventAdmin = context?.getService(EventAdmin)
            if (eventAdmin == null) {
                return
            }
            String machineId = context?.getMachineId()
            if (machineId == null || machineId.trim().isEmpty()) {
                machineId = String.valueOf(ctx?.getMachineName() ?: "")
            }
            if (machineId == null || machineId.trim().isEmpty()) {
                return
            }

            String eventDedupKey = String.valueOf(phase) + "\u0001" + String.valueOf(transition ?: "") + "\u0001" + String.valueOf(reason ?: "")
            String previousDedup = String.valueOf(ctx.getPersistentData().getOrDefault(KEY_UI_LOGIN_PHASE_EVENT_DEDUP, ""))
            if (eventDedupKey == previousDedup) {
                return
            }
            ctx.getPersistentData().put(KEY_UI_LOGIN_PHASE_EVENT_DEDUP, eventDedupKey)
            ctx.getPersistentData().put("uiLoginPhase", phase)

            def props = [
                    machineId    : machineId,
                    transition   : transition ?: "EnginePhase",
                    connected    : Boolean.valueOf(ctx.getDispatcher().isServerConnected()),
                    authenticated: Boolean.valueOf(isAuthenticated(state)),
                    loginPhase   : phase,
                    timestamp    : System.currentTimeMillis()
            ] as Map<String, Object>
            if (reason != null && !reason.isEmpty()) {
                props.put("reason", reason)
            }
            int retryCount = 0
            try {
                def retryRaw = ctx.getPersistentData().getOrDefault("loginRetryAttempt", 0)
                retryCount = (retryRaw instanceof Number) ? ((Number) retryRaw).intValue() : toInt(retryRaw, 0)
            } catch (Exception ignored) {}
            props.put("retryCount", Math.max(0, retryCount))

            int maxRetries = 0
            int configuredClientVersion = 188
            try {
                def runtime = resolveRuntimeSettings(ctx, loginSettingsProvider, false)
                if (runtime != null) {
                    maxRetries = Math.max(0, toInt(runtime.maxRetryAttempts, 0))
                    configuredClientVersion = toInt(runtime.gatewayClientVersion, 188)
                }
            } catch (Exception ignored) {}
            props.put("maxRetries", maxRetries)
            props.put("configuredClientVersion", configuredClientVersion)
            String lastFailureReason = null
            try {
                lastFailureReason = String.valueOf(
                        (reason != null && !reason.isEmpty())
                                ? reason
                                : (state?.getFailureReason() ?: "")
                )
            } catch (Exception ignored) {}
            if (lastFailureReason != null && !lastFailureReason.isEmpty()) {
                props.put("lastFailureReason", lastFailureReason)
            }
            if (extras != null) {
                props.putAll(extras)
            }
            eventAdmin.postEvent(new Event("sokybot/network/${osgiEventTopicSeg(machineId)}/EnginePhase", props))
        } catch (Exception ignored) {
            // Never fail cycle execution on telemetry/event updates.
        }
    }

    /** Resolves engine phase strings to enum values for idempotent model updates (null if not an enum constant). */
    private LoginState.Phase resolveLoginPhaseEnum(String phase) {
        if (phase == null) return null
        try {
            return LoginState.Phase.valueOf(phase)
        } catch (IllegalArgumentException ignored) {
            return null
        }
    }

    private void setLoginStatePhase(def loginState, String phase) {
        if (loginState == null || phase == null) return
        try {
            try {
                loginState.setPhase(LoginState.Phase.valueOf(phase))
                return
            } catch (IllegalArgumentException ignored) {
                // non-enum aliases are handled below
            }
            switch (phase) {
                case PHASE_MISSING_GATEWAY:
                    loginState.setPhase(LoginState.Phase.MISSING_GATEWAY); return
                case PHASE_PENDING_MANUAL_CONNECT:
                    loginState.setPhase(LoginState.Phase.PENDING_MANUAL_CONNECT); return
                case PHASE_MISSING_CREDENTIALS:
                    loginState.setPhase(LoginState.Phase.MISSING_CREDENTIALS); return
                case PHASE_MISSING_AGENT_SERVER:
                    loginState.setPhase(LoginState.Phase.MISSING_AGENT_SERVER); return
                case PHASE_MISSING_CHARACTER_SELECTION:
                    loginState.setPhase(LoginState.Phase.MISSING_CHARACTER_SELECTION); return
                case PHASE_WAITING_FOR_AGENTS:
                    loginState.setPhase(LoginState.Phase.WAITING_FOR_AGENTS); return
                case PHASE_GATEWAY_LOGIN_PAUSE:
                    loginState.setPhase(LoginState.Phase.AGENTS_RECEIVED); return
                case PHASE_WAITING_FOR_AGENTS_TIMEOUT:
                    loginState.setPhase(LoginState.Phase.WAITING_FOR_AGENTS_TIMEOUT); return
                case PHASE_WAITING_FOR_PASSCODE:
                    loginState.setPhase(LoginState.Phase.WAITING_FOR_PASSCODE); return
                case PHASE_WAIT_FOR_CAPTCHA:
                    loginState.setPhase(LoginState.Phase.WAIT_FOR_CAPTCHA); return
                case PHASE_PASSCODE_SUBMITTED:
                    loginState.setPhase(LoginState.Phase.PASSCODE_SUBMITTED); return
                case PHASE_IN_QUEUE:
                    loginState.setPhase(LoginState.Phase.IN_QUEUE); return
                case PHASE_LOADING_ENVIRONMENT:
                    loginState.setPhase(LoginState.Phase.LOADING_ENVIRONMENT); return
                case "CONNECTING_GATEWAY":
                    loginState.setPhase(LoginState.Phase.CONNECTING_GATEWAY); return
                case "MANUAL_VERIFICATION_REQUIRED":
                    loginState.setPhase(LoginState.Phase.MANUAL_VERIFICATION_REQUIRED); return
                case "RETRY_DELAY":
                    loginState.setPhase(LoginState.Phase.RETRY_DELAY); return
                case "RETRY_DISABLED":
                    loginState.setPhase(LoginState.Phase.RETRY_DISABLED); return
                case "RETRY_LIMIT_REACHED":
                    loginState.setPhase(LoginState.Phase.RETRY_LIMIT_REACHED); return
                default:
                    return
            }
        } catch (Exception ignored) {
            // Ignore unknown/non-enum phases
        }
    }

    @Override
    void shutdown(IActuatorContext ctx) {
        try {
            gracefulShutdown(ctx)
            def data = ctx?.getPersistentData()
            if (data != null) {
                data.remove(KEY_RETRY_UNTIL_MS)
                data.remove(KEY_RETRY_ATTEMPT_ID)
                data.remove(KEY_ATTEMPT_LOGIN_SETTINGS)
                data.remove(KEY_INTERACTIVE_WAIT_UNTIL_MS)
                data.remove(KEY_USER_RESUME_REQUIRED)
            }
            clearVolatileAttributes(ctx)
        } catch (Exception ignored) {
        }
        super.shutdown(ctx)
    }
}

class LoginSettings {
    String targetGateway = ""
    @Encrypted String username = ""
    @Encrypted String password = ""
    @Encrypted String passcode = ""
    String targetAgent = ""
    String selectedCharacter = ""
    int locale = 22
    boolean autoLogin = false
    boolean autoReconnect = true
    int retryBaseDelayMs = 5000
    int retryMaxDelayMs = 60000
    int maxRetryAttempts = 0
    boolean infiniteRetryMode = true
    String loginCharset = "windows-1252"
    int agentWaitTimeoutMs = 15000
    int loginResponseTimeoutMs = 15000
    int agentAuthTimeoutMs = 30000
    int passcodeWaitTimeoutMs = 60000
    int passcodeUserInputTimeoutMs = 60000
    int agentRequestMaxRetries = 2
    int agentRequestRetryBackoffMs = 2000
    int agentBanBlockDurationMs = 300000
    /** Minimum milliseconds between gateway 0x6102 sends (0 = disabled). Default reduces flood / anti-DDoS triggers. */
    int gatewayLoginMinIntervalMs = 2000
    /** Milliseconds to wait after agent list before sending 0x6102 (0 = only engine minimum ~10 ms). Default humanizes timing. */
    int gatewayLoginPauseAfterAgentListMs = 1500
    /** Gateway 0x6100 client build (e.g. 0xBC = 188 on many vSRO-style gateways). */
    int gatewayClientVersion = 188
    /** UTF-8 module name in 0x6100 (default SR_Client). */
    String gatewayClientModule = "SR_Client"
    int selectedCharacterSlot = -1
    int characterSlotBase = 0
    boolean characterSelectionStrictMode = false
    boolean passcodeStringDetectionEnabled = true
    int logoutAckTimeoutMs = 3000
}

new Login()
