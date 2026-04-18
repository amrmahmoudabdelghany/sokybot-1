import org.sokybot.settings.security.Encrypted
import org.sokybot.gamemodel.LoginState
import org.sokybot.engine.api.login.GatewayCredentials
import org.sokybot.engine.api.login.IGatewayProtocolEmitter
import org.sokybot.engine.api.login.ILoginFailureClassifier
import org.sokybot.engine.api.login.ILoginInteractiveCoordinator
import org.sokybot.engine.api.login.ILoginSettingsSnapshotter
import org.sokybot.engine.api.login.InteractiveOutcome
import org.sokybot.engine.api.login.LoginFailureClass
import org.sokybot.engine.api.login.LoginPhaseAliases
import org.osgi.service.event.Event
import org.osgi.service.event.EventAdmin
import java.util.concurrent.ConcurrentHashMap

class Login extends BaseActuator {
    private def loginSettingsProvider
    private static final List<Long> RETRY_DELAYS_MS = [5000L, 10000L, 30000L, 60000L]
    private static final long MIN_RETRY_DELAY_MS = 5000L
    private static final long MAX_RETRY_DELAY_MS = 300000L
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

    private static final long AGENT_REQUEST_MIN_INTERVAL_MS = 10_000L

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
                            if (settings != null) {
                                beginAttemptSnapshot(ctx, settings)
                            }
                            String host = normalizeHostForSocket((String) ctx.getPersistentData().get("gatewayHost"))
                            int port = (int) ctx.getPersistentData().get("gatewayPort")
                            emitEnginePhase(ctx, "CONNECTING_GATEWAY", "Connecting", null)
                            logInfoCtx(ctx, "CONNECT_TO_GATEWAY attempting -> {}:{} (alreadyConnected={})",
                                    host, port, ctx.getDispatcher().isServerConnected())
                            applyGatewayHandshakeHintsToProxy(ctx, settings)
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
                            def verdict = classifyFailureVerdict(loginState, ctx)
                            String failureClass = verdict?.getFailureClass() != null ? String.valueOf(verdict.getFailureClass().name()) : classifyFailure(loginState, ctx)
                            ctx.getPersistentData().remove(KEY_LOGIN_RESPONSE_DEADLINE_MS)
                            emitEnginePhase(ctx, LoginState.Phase.FAILED.name(), "GatewayLoginFailed",
                                    String.valueOf(loginState?.getFailureReason() ?: ""),
                                    [failureClass: failureClass, gatewayResultCode: verdict?.getGatewayCode()])
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

                            def verdict = classifyFailureVerdict(loginState, ctx)
                            String failureClass = verdict?.getFailureClass() != null ? String.valueOf(verdict.getFailureClass().name()) : classifyFailure(loginState, ctx)
                            logInfoCtx(ctx, "Failure classified as {}", failureClass)

                            // (d) terminal halts / special cases
                            if (handleTerminalFailure(ctx, loginState, verdict, settings)) return

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

    private IGatewayProtocolEmitter gatewayProtocolEmitter(def ctx) {
        return ctx?.getServiceOptional(IGatewayProtocolEmitter)?.orElse(null)
    }

    private ILoginSettingsSnapshotter loginSettingsSnapshotter(def ctx) {
        return ctx?.getServiceOptional(ILoginSettingsSnapshotter)?.orElse(null)
    }

    private ILoginFailureClassifier loginFailureClassifier(def ctx) {
        return ctx?.getServiceOptional(ILoginFailureClassifier)?.orElse(null)
    }

    private ILoginInteractiveCoordinator loginInteractiveCoordinator(def ctx) {
        return ctx?.getServiceOptional(ILoginInteractiveCoordinator)?.orElse(null)
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
    private void applyGatewayHandshakeHintsToProxy(def ctx, def settings) {
        try {
            def proxy = ctx?.getProxyConnection()
            if (proxy == null) return
            def s = settings
            byte loc = effectiveGatewayLocale(s)
            String mod = normalizeGatewayClientModule(s?.gatewayClientModule)
            int ver = toInt(s?.gatewayClientVersion, 188)
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
        if (settings?.gatewayLoginMinIntervalMs instanceof Number) {
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
        if (settings?.gatewayLoginPauseAfterAgentListMs instanceof Number) {
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

    private def resolveRuntimeSettings(def ctx, def settingsProvider, boolean fetchFromProvider) {
        boolean explicitConnect = Boolean.TRUE.equals(ctx.getPersistentData().get("explicitConnectRequested"))
        def snapshotter = loginSettingsSnapshotter(ctx)
        if (snapshotter == null) {
            return null
        }
        boolean forceRefresh = explicitConnect || fetchFromProvider
        def snapshot = snapshotter.snapshot(ctx, forceRefresh)
        if (explicitConnect) {
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
        }
        return snapshot
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
        if (settings == null) return
        ctx.getPersistentData().put(KEY_ATTEMPT_LOGIN_SETTINGS, settings)
        long current = currentAttemptId(ctx)
        ctx.getPersistentData().put(KEY_ACTIVE_ATTEMPT_ID, current + 1L)
    }

    private long effectiveAgentWaitTimeoutMs(def settings) {
        if (settings?.agentWaitTimeoutMs instanceof Number) return ((Number) settings.agentWaitTimeoutMs).longValue()
        if (settings?.getAgentWaitTimeoutMs() != null) return (long) settings.getAgentWaitTimeoutMs()
        return AGENT_WAIT_TIMEOUT_MS
    }

    private long effectiveLoginResponseTimeoutMs(def settings) {
        if (settings?.loginResponseTimeoutMs instanceof Number) return ((Number) settings.loginResponseTimeoutMs).longValue()
        if (settings?.getLoginResponseTimeoutMs() != null) return (long) settings.getLoginResponseTimeoutMs()
        return LOGIN_RESPONSE_TIMEOUT_MS
    }

    private long effectiveAgentAuthTimeoutMs(def settings) {
        if (settings?.agentAuthTimeoutMs instanceof Number) return ((Number) settings.agentAuthTimeoutMs).longValue()
        if (settings?.getAgentAuthTimeoutMs() != null) return (long) settings.getAgentAuthTimeoutMs()
        return AGENT_AUTH_TIMEOUT_MS
    }

    private long effectivePasscodeUserInputTimeoutMs(def settings) {
        if (settings?.passcodeUserInputTimeoutMs instanceof Number) return ((Number) settings.passcodeUserInputTimeoutMs).longValue()
        if (settings?.getPasscodeUserInputTimeoutMs() != null) return (long) settings.getPasscodeUserInputTimeoutMs()
        return PASSCODE_USER_INPUT_TIMEOUT_MS
    }

    /**
     * Requests 0x6101 through IGatewayProtocolEmitter with cache/ban guards.
     */
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
            def protocol = gatewayProtocolEmitter(ctx)
            if (protocol == null) {
                log.warn("Gateway protocol emitter unavailable; cannot request agent list")
                return false
            }
            return protocol.requestAgentList(ctx, allowBypass, AGENT_REQUEST_MIN_INTERVAL_MS)
        } catch (Exception e) {
            long cooldown = (settings?.agentRequestRetryBackoffMs instanceof Number) ? ((Number) settings.agentRequestRetryBackoffMs).longValue() : 2000L
            ctx.getPersistentData().put(KEY_AGENT_BYPASS_COOLDOWN_UNTIL_MS, now + cooldown)
            log.warn("Agent request failed: {}", e.getMessage())
            return false
        }
    }

    private void sendLoginRequest(def ctx, byte locale, String username, String password, short agentId, String charsetName) {
        def protocol = gatewayProtocolEmitter(ctx)
        if (protocol == null) {
            throw new IllegalStateException("Gateway protocol emitter unavailable")
        }
        protocol.sendLoginRequest(ctx, new GatewayCredentials(username, password), locale, (int) agentId & 0xFFFF, charsetName, true)
    }

    private void sendGatewayImageCodeAnswer(def ctx, String answer) {
        def protocol = gatewayProtocolEmitter(ctx)
        if (protocol == null) {
            throw new IllegalStateException("Gateway protocol emitter unavailable")
        }
        protocol.sendGatewayImageCodeAnswer(ctx, answer)
    }


    private def currentAttemptSettings(def ctx, def settingsProvider) {
        def existing = ctx.getPersistentData().get(KEY_ATTEMPT_LOGIN_SETTINGS)
        if (existing != null) {
            return existing
        }
        def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
        if (settings != null) {
            beginAttemptSnapshot(ctx, settings)
        }
        return settings
    }

    private long currentAttemptId(def ctx) {
        def raw = ctx.getPersistentData().get(KEY_ACTIVE_ATTEMPT_ID)
        if (raw instanceof Number) return ((Number) raw).longValue()
        return 0L
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
        def protocol = gatewayProtocolEmitter(ctx)
        if (protocol == null) {
            return
        }
        def settings = resolveRuntimeSettings(ctx, loginSettingsProvider, false)
        long timeout = (settings?.logoutAckTimeoutMs instanceof Number) ? ((Number) settings.logoutAckTimeoutMs).longValue() : LOGOUT_ACK_TIMEOUT_MS
        protocol.gracefulShutdown(ctx, timeout)
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
        def coordinator = loginInteractiveCoordinator(ctx)
        if (coordinator == null) {
            return false
        }
        InteractiveOutcome outcome = coordinator.handle(ctx, settings)
        if (outcome == InteractiveOutcome.WAITING_FOR_PASSCODE) {
            emitEnginePhase(ctx, PHASE_WAITING_FOR_PASSCODE, "WaitingForPasscode", String.valueOf(snap?.failureReason ?: ""))
            return true
        }
        if (outcome == InteractiveOutcome.WAITING_FOR_CAPTCHA) {
            emitEnginePhase(ctx, PHASE_WAIT_FOR_CAPTCHA, "WaitingForPasscode", String.valueOf(snap?.failureReason ?: ""))
            return true
        }
        if (outcome == InteractiveOutcome.TIMED_OUT_DISCONNECT) {
            return false
        }
        return false
    }

    private boolean handleQueueWait(def ctx, def loginState) {
        def coordinator = loginInteractiveCoordinator(ctx)
        if (coordinator == null) return false
        InteractiveOutcome outcome = coordinator.handle(ctx, resolveRuntimeSettings(ctx, loginSettingsProvider, false))
        if (outcome == InteractiveOutcome.IN_QUEUE) {
            emitEnginePhase(ctx, PHASE_IN_QUEUE, "QueueWaiting", String.valueOf(loginState?.getFailureReason() ?: ""),
                    [queuePosition: parseQueuePosition(loginState)])
            return true
        }
        return false
    }

    private boolean handleMissingPrereq(def ctx, def loginState) {
        def coordinator = loginInteractiveCoordinator(ctx)
        if (coordinator == null) return false
        InteractiveOutcome outcome = coordinator.handle(ctx, resolveRuntimeSettings(ctx, loginSettingsProvider, false))
        if (outcome == InteractiveOutcome.WAITING_FOR_USER_RESUME) {
            ctx.getPersistentData().remove(KEY_RETRY_UNTIL_MS)
            ctx.getPersistentData().remove(KEY_RETRY_ATTEMPT_ID)
            return true
        }
        return false
    }

    private boolean handleTerminalFailure(def ctx, def loginState, def verdict, def settings) {
        String failureClass = verdict?.getFailureClass() != null ? String.valueOf(verdict.getFailureClass().name()) : FAILURE_UNKNOWN_RETRY
        if (LoginFailureClass.MISSING_PREREQ.name().equals(failureClass)) {
            ctx.getPersistentData().put(KEY_USER_RESUME_REQUIRED, true)
            return true
        }
        String haltSignature = buildHaltSignature(ctx, loginState, failureClass)
        String lastHaltSignature = String.valueOf(ctx.getPersistentData().getOrDefault(KEY_LOGIN_LAST_HALT_SIGNATURE, ""))
        boolean haltAlreadyLogged = haltSignature == lastHaltSignature

        if (LoginFailureClass.MANUAL_VERIFICATION.name().equals(failureClass)) {
            emitEnginePhase(ctx, "MANUAL_VERIFICATION_REQUIRED", "ManualVerification", String.valueOf(loginState?.getFailureReason() ?: ""),
                    [failureClass: failureClass, fatal: true])
            if (!haltAlreadyLogged) {
                log.warn("Login paused: manual verification required ({})", loginState?.getFailureReason())
                ctx.getPersistentData().put(KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature)
            }
            return true
        }
        if (LoginFailureClass.CREDENTIAL.name().equals(failureClass)) {
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
        if (LoginFailureClass.CHARACTER_NOT_FOUND.name().equals(failureClass)) {
            emitEnginePhase(ctx, LoginState.Phase.FAILED.name(), "CharacterUnavailable",
                    String.valueOf(loginState?.getFailureReason() ?: "CHARACTER_SELECTION_UNAVAILABLE"),
                    [failureClass: failureClass, fatal: true])
            return true
        }
        if (LoginFailureClass.AGENT_TIMEOUT.name().equals(failureClass)) {
            emitEnginePhase(ctx, PHASE_WAITING_FOR_AGENTS_TIMEOUT, "WaitingForAgentsTimeout",
                    String.valueOf(loginState?.getFailureReason() ?: "Agent list timeout"))
        }
        if (LoginFailureClass.SERVER_INSPECTION.name().equals(failureClass)) {
            long now = System.currentTimeMillis()
            long delayMs = Math.max(nextRetryDelayMs(ctx, settings), verdict?.getRetryDelayFloorMs() instanceof Number ? (long) verdict.getRetryDelayFloorMs() : 60000L)
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
        if (LoginFailureClass.GHOST_COOLDOWN.name().equals(failureClass)) {
            delayMs = Math.max(delayMs, 60000L)
        }
        boolean infiniteRequested = settings != null && toBool(settings.infiniteRetryMode, true)
        boolean infiniteEffective = ((LoginFailureClass.NETWORK.name().equals(failureClass) || LoginFailureClass.AGENT_TIMEOUT.name().equals(failureClass))) && infiniteRequested
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

    private String redactForLog(String username) {
        if (username == null || username.isEmpty()) return "<empty>"
        if (username.length() == 1) return "*"
        if (username.length() == 2) return username.charAt(0) + "*"
        return username.substring(0, 1) + "***" + username.substring(username.length() - 1)
    }

    private def classifyFailureVerdict(def loginState, def ctx) {
        def classifier = loginFailureClassifier(ctx)
        if (classifier == null) {
            return null
        }
        return classifier.classify(ctx, String.valueOf(loginState?.getFailureReason() ?: ""))
    }

    private String classifyFailure(def loginState, def ctx) {
        def verdict = classifyFailureVerdict(loginState, ctx)
        if (verdict == null || verdict.getFailureClass() == null) {
            return FAILURE_UNKNOWN_RETRY
        }
        return String.valueOf(verdict.getFailureClass().name())
    }

    private String credentialFailureDetail(def loginState) {
        String r = String.valueOf(loginState?.getFailureReason() ?: "")
        return r == null ? "" : r
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

    /** Resolves engine phase strings to enum values for idempotent model updates (null if not known). */
    private LoginState.Phase resolveLoginPhaseEnum(String phase) {
        if (phase == null) return null
        return LoginPhaseAliases.resolve(phase).orElse(null)
    }

    private void setLoginStatePhase(def loginState, String phase) {
        if (loginState == null || phase == null) return
        try {
            LoginPhaseAliases.resolve(phase).ifPresent { p -> loginState.setPhase(p) }
        } catch (Exception ignored) {
            // Ignore unknown/non-enum phases
        }
    }

    /** @deprecated Retained for script/plugin cross-references; prefer LoginPhaseAliases. */
    @Deprecated
    private static final Map<String, LoginState.Phase> LEGACY_PHASE_ALIASES = LoginPhaseAliases.defaultAliases()

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
