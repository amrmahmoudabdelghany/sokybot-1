import org.sokybot.settings.security.Encrypted
import org.sokybot.gamemodel.LoginState
import org.osgi.service.event.Event
import org.osgi.service.event.EventAdmin

class Login extends BaseActuator {
    private static final List<Long> RETRY_DELAYS_MS = [5000L, 10000L, 30000L, 60000L]
    private static final long MIN_RETRY_DELAY_MS = 5000L
    private static final long MAX_RETRY_DELAY_MS = 300000L
    private static final String KEY_LOGIN_HALTED_CREDENTIAL = "loginHaltedCredentialFailure"
    private static final String KEY_LOGIN_LAST_HALT_SIGNATURE = "loginLastHaltSignature"
    private static final String FAILURE_NETWORK = "NETWORK"
    private static final String FAILURE_AGENT_TIMEOUT = "AGENT_TIMEOUT"
    private static final String FAILURE_CREDENTIAL = "CREDENTIAL"
    private static final String FAILURE_MANUAL_VERIFICATION = "MANUAL_VERIFICATION"
    private static final String PHASE_MISSING_GATEWAY = "MISSING_GATEWAY"
    private static final String PHASE_MISSING_CREDENTIALS = "MISSING_CREDENTIALS"
    private static final String PHASE_MISSING_AGENT_SERVER = "MISSING_AGENT_SERVER"
    private static final String PHASE_MISSING_CHARACTER_SELECTION = "MISSING_CHARACTER_SELECTION"
    private static final String PHASE_WAITING_FOR_AGENTS = "WAITING_FOR_AGENTS"
    private static final String PHASE_WAITING_FOR_AGENTS_TIMEOUT = "WAITING_FOR_AGENTS_TIMEOUT"
    private static final String KEY_AGENT_WAIT_DEADLINE_MS = "loginAgentWaitDeadlineMs"
    private static final String KEY_AGENT_WAIT_TIMED_OUT = "loginAgentWaitTimedOut"
    private static final String KEY_LOGIN_RESPONSE_DEADLINE_MS = "loginResponseDeadlineMs"
    private static final String KEY_AGENT_AUTH_DEADLINE_MS = "agentAuthDeadlineMs"
    private static final long AGENT_WAIT_TIMEOUT_MS = 15000L
    private static final long LOGIN_RESPONSE_TIMEOUT_MS = 15000L
    private static final long AGENT_AUTH_TIMEOUT_MS = 45000L

    /** OSGi Event topics reject {@code '.'} in segments (machine ids are {@code Group.Machine}). */
    private static String osgiEventTopicSeg(String s) {
        if (s == null || s.isEmpty()) return '_'
        return s.replaceAll(/[^A-Za-z0-9_-]/, '_')
    }

    Login() { super("login") }

    @Override
    void setup() {
        registerSettings("login", LoginSettings, { new LoginSettings() })

        def settingsProvider = settingsProvider("login", LoginSettings)
        if (settingsProvider == null) {
            log.warn("Login: settings provider not available; cycle will not be registered")
            return
        }

        def cycle = new CycleDefinitionBuilder()
                .name("login-cycle")
                .priority(200)
                .entryState("CHECK_CONNECTION")
                .entryGuard({ ctx ->
                    boolean explicitConnect = Boolean.TRUE.equals(ctx.getPersistentData().get("explicitConnectRequested"))
                    def settings = resolveRuntimeSettings(ctx, settingsProvider, explicitConnect)
                    def loginState = ctx.getGameModel()?.getLoginState()
                    if (settings == null || parseGateway(String.valueOf(settings?.targetGateway ?: "")) == null) {
                        emitEnginePhase(ctx, PHASE_MISSING_GATEWAY, "MissingRequirement", null)
                        return false
                    }
                    boolean haltedByCredential = Boolean.TRUE.equals(ctx.getPersistentData().get(KEY_LOGIN_HALTED_CREDENTIAL))
                    boolean result = settings != null &&
                            (toBool(settings?.autoLogin, false) || explicitConnect) &&
                            !requiresManualVerification(loginState) &&
                            !isAuthenticated(loginState) &&
                            !isLoggedIn(ctx) &&
                            !haltedByCredential
                    if (!result) {
                        log.info("Login entry guard FAILED: settings={}, autoLogin={}, explicitConnect={}, requiresVerification={}, isAuthenticated={}, isLoggedIn={}, haltedByCredential={}",
                                settings != null, toBool(settings?.autoLogin, false), explicitConnect,
                                requiresManualVerification(loginState), isAuthenticated(loginState), isLoggedIn(ctx), haltedByCredential)
                    }
                    return result
                })
                .state("CHECK_CONNECTION", { builder -> builder
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
                            def gateway = parseGateway(settings?.targetGateway)
                            if (gateway != null) {
                                emitEnginePhase(ctx, "CONNECTING_GATEWAY", "Connecting", null)
                                ctx.getPersistentData().put("gatewayHost", gateway.host)
                                ctx.getPersistentData().put("gatewayPort", gateway.port)
                                ctx.getPersistentData().put("loginLastGatewayRaw", String.valueOf(settings?.targetGateway ?: ""))
                                log.info("CHECK_CONNECTION resolved gateway for machine {} -> {}:{} (raw='{}')",
                                        ctx.getMachineName(), gateway.host, gateway.port, String.valueOf(settings?.targetGateway ?: ""))
                            } else {
                                ctx.getPersistentData().remove("gatewayHost")
                                ctx.getPersistentData().remove("gatewayPort")
                                log.warn("CHECK_CONNECTION gateway parse failed for machine {} (raw='{}'), skipping connect",
                                        ctx.getMachineName(), String.valueOf(settings?.targetGateway ?: ""))
                            }
                        })
                        .nextState("CONNECT_TO_GATEWAY")
                        .targetState("POST_CHECK_CONNECTION")
                })
                .conditionalState("POST_CHECK_CONNECTION", { cb ->
                    cb.when({ ctx ->
                        def phase = ctx.getGameModel()?.getLoginState()?.getPhase()
                        boolean midRedirect = phase == LoginState.Phase.LOGIN_SUCCESS || phase == LoginState.Phase.REDIRECTING
                                || phase == LoginState.Phase.AGENT_CONNECTED
                        midRedirect && !ctx.getDispatcher().isServerConnected()
                    }, "WAIT_FOR_LOGIN_RESPONSE")
                            .when({ ctx -> ctx.getDispatcher().isServerConnected() }, "CONNECTED_RESUME")
                            .defaultTo("RETRY_DELAY")
                })
                .conditionalState("CONNECTED_RESUME", { cb ->
                    cb.when({ ctx ->
                        def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                        def ls = ctx.getGameModel()?.getLoginState()
                        loginPrereqsForGateway(settings, ls)
                    }, "SEND_LOGIN_REQUEST")
                            .when({ ctx -> hasAgents(ctx.getGameModel()?.getLoginState()) }, "PARK_MISSING_LOGIN")
                            .defaultTo("RESEND_AGENT_LIST_REQUEST")
                })
                .state("RESEND_AGENT_LIST_REQUEST", { builder -> builder
                        .guard({ ctx -> true })
                        .action({ ctx ->
                            resetRetry(ctx)
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_DEADLINE_MS, System.currentTimeMillis() + AGENT_WAIT_TIMEOUT_MS)
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_TIMED_OUT, false)
                            emitEnginePhase(ctx, PHASE_WAITING_FOR_AGENTS, "WaitingForAgents", null)
                            log.info("Already connected to gateway; (re-)requesting agent list for machine {}", ctx.getMachineName())
                            sendToServer(0, ClientOpcode.AGENT_REQUEST) { it }
                        })
                        .nextState("WAIT_FOR_AGENTS")
                        .targetState("RETRY_DELAY")
                })
                .state("PARK_MISSING_LOGIN", { builder -> builder
                        .guard({ ctx -> true })
                        .action({ ctx ->
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            def loginState = ctx.getGameModel()?.getLoginState()
                            if (settings == null) return
                            if (isBlank(settings.username) || isBlank(settings.password) || isBlank(settings.passcode)) {
                                emitEnginePhase(ctx, PHASE_MISSING_CREDENTIALS, "MissingRequirement", null)
                            } else if (hasAgents(loginState) && isBlank(settings.targetAgent)) {
                                emitEnginePhase(ctx, PHASE_MISSING_AGENT_SERVER, "MissingRequirement", null)
                            }
                            log.info("Machine {} gateway session holds agent list; waiting for user to complete login prerequisites (no 0x6102 until then, so 0xA102 will not appear)",
                                    ctx.getMachineName())
                        })
                        .nextState(null)
                        .targetState("RETRY_DELAY")
                })
                .state("CONNECT_TO_GATEWAY", { builder -> builder
                        .guard({ ctx -> ctx.getPersistentData().get("gatewayHost") != null })
                        .action({ ctx ->
                            String host = (String) ctx.getPersistentData().get("gatewayHost")
                            int port = (int) ctx.getPersistentData().get("gatewayPort")
                            emitEnginePhase(ctx, "CONNECTING_GATEWAY", "Connecting", null)
                            log.info("CONNECT_TO_GATEWAY attempting machine {} -> {}:{} (alreadyConnected={})",
                                    ctx.getMachineName(), host, port, ctx.getDispatcher().isServerConnected())
                            ctx.getDispatcher().setClientlessMode(true)
                            ctx.getDispatcher().connect(host, port)
                            log.info("CONNECT_TO_GATEWAY result machine {} connected={}",
                                    ctx.getMachineName(), ctx.getDispatcher().isServerConnected())
                        })
                        .nextState("WAIT_FOR_CONNECTION")
                        .targetState("RETRY_DELAY")
                })
                .state("WAIT_FOR_CONNECTION", { builder -> builder
                        .guard({ ctx -> ctx.getDispatcher().isServerConnected() })
                        .action({ ctx ->
                            resetRetry(ctx)
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_DEADLINE_MS, System.currentTimeMillis() + AGENT_WAIT_TIMEOUT_MS)
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_TIMED_OUT, false)
                            emitEnginePhase(ctx, PHASE_WAITING_FOR_AGENTS, "WaitingForAgents", null)
                            log.info("Gateway connected, requesting agent list")
                            sendToServer(0, ClientOpcode.AGENT_REQUEST) { it }
                        })
                        .nextState("WAIT_FOR_AGENTS")
                        .targetState("RETRY_DELAY")
                })
                .state("WAIT_FOR_AGENTS", { builder -> builder
                        .guard({ ctx -> hasAgents(ctx.getGameModel()?.getLoginState()) })
                        .action({ ctx ->
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_TIMED_OUT, false)
                            log.info("Agent list received, sending login request")
                        })
                        .nextState("SEND_LOGIN_REQUEST")
                        .targetState("CHECK_AGENT_WAIT_TIMEOUT")
                })
                .state("CHECK_AGENT_WAIT_TIMEOUT", { builder -> builder
                        .guard({ ctx -> isAgentWaitTimedOut(ctx) })
                        .action({ ctx ->
                            ctx.getPersistentData().put(KEY_AGENT_WAIT_TIMED_OUT, true)
                            emitEnginePhase(ctx, PHASE_WAITING_FOR_AGENTS_TIMEOUT, "WaitingForAgentsTimeout", null)
                            def loginState = ctx.getGameModel()?.getLoginState()
                            if (loginState != null) {
                                loginState.setFailureReason("Agent list timeout")
                            }
                            log.warn("Agent list wait timed out after {} ms; entering retry flow", AGENT_WAIT_TIMEOUT_MS)
                        })
                        .nextState("RETRY_DELAY")
                        .targetState("WAIT_FOR_AGENTS_RECHECK")
                })
                .delayState("WAIT_FOR_AGENTS_RECHECK", { builder -> builder
                        .delay(500)
                        .nextState("WAIT_FOR_AGENTS")
                })
                .state("SEND_LOGIN_REQUEST", { builder -> builder
                        .guard({ ctx ->
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            def loginState = ctx.getGameModel()?.getLoginState()
                            if (settings == null) return false
                            if (isBlank(settings.username) || isBlank(settings.password) || isBlank(settings.passcode)) {
                                emitEnginePhase(ctx, PHASE_MISSING_CREDENTIALS, "MissingRequirement", null)
                                return false
                            }
                            if (hasAgents(loginState) && isBlank(settings.targetAgent)) {
                                emitEnginePhase(ctx, PHASE_MISSING_AGENT_SERVER, "MissingRequirement", null)
                                return false
                            }
                            return true
                        })
                        .action({ ctx ->
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            if (settings == null) return
                            String username = String.valueOf(settings.username ?: "")
                            String password = String.valueOf(settings.password ?: "")
                            log.info("Sending login request for user: {}", username)
                            ctx.getGameModel()?.getLoginState()?.setPhase(LoginState.Phase.LOGIN_SENT)
                            emitEnginePhase(ctx, LoginState.Phase.LOGIN_SENT.name(), "LoginSent", null)
                            long now = System.currentTimeMillis()
                            ctx.getPersistentData().put(KEY_LOGIN_RESPONSE_DEADLINE_MS, now + LOGIN_RESPONSE_TIMEOUT_MS)
                            ctx.getPersistentData().put(KEY_AGENT_AUTH_DEADLINE_MS, now + AGENT_AUTH_TIMEOUT_MS)

                            byte locale = (byte) toInt(settings.locale, 22)
                            short agentId = parseAgentId(String.valueOf(settings.targetAgent ?: ""))

                            int packetLen = 7 + username.length() + password.length()
                            sendToServer(packetLen, ClientOpcode.LOGIN_REQUEST) { it
                                .put(locale)
                                .putShort((short) username.length())
                                .putBytes(username.getBytes())
                                .putShort((short) password.length())
                                .putBytes(password.getBytes())
                                .putShort(agentId)
                            }
                        })
                        .nextState("WAIT_FOR_LOGIN_RESPONSE")
                        .targetState("RETRY_DELAY")
                })
                .state("WAIT_FOR_LOGIN_RESPONSE", { builder -> builder
                        .guard({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            def phase = loginState?.getPhase()
                            if (phase == LoginState.Phase.LOGIN_SUCCESS || phase == LoginState.Phase.REDIRECTING
                                    || phase == LoginState.Phase.AGENT_CONNECTED
                                    || phase == LoginState.Phase.AUTHENTICATED) {
                                return true
                            }
                            return isLoggedIn(ctx)
                        })
                        .action({ ctx ->
                            log.info("Machine {} gateway login response received (phase={})",
                                    ctx.getMachineName(), ctx.getGameModel()?.getLoginState()?.getPhase())
                        })
                        .nextState("WAIT_FOR_AGENT_AUTH")
                        .targetState("CHECK_LOGIN_TIMEOUT")
                })
                .state("CHECK_LOGIN_TIMEOUT", { builder -> builder
                        .guard({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            if (loginState?.getPhase() == LoginState.Phase.FAILED) {
                                return true
                            }
                            return isLoginResponseTimedOut(ctx) && loginState?.getPhase() == LoginState.Phase.LOGIN_SENT
                        })
                        .action({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            if (loginState?.getPhase() == LoginState.Phase.FAILED) {
                                log.warn("Machine {} gateway login failed: {}", ctx.getMachineName(),
                                        loginState?.getFailureReason() ?: "")
                            } else {
                                if (loginState != null) {
                                    loginState.setFailureReason("Gateway login response timeout")
                                }
                                log.warn("Gateway login response timed out after {} ms", LOGIN_RESPONSE_TIMEOUT_MS)
                            }
                        })
                        .nextState("RETRY_DELAY")
                        .targetState("WAIT_FOR_LOGIN_RECHECK")
                })
                .delayState("WAIT_FOR_LOGIN_RECHECK", { builder -> builder
                        .delay(500)
                        .nextState("WAIT_FOR_LOGIN_RESPONSE")
                })
                .state("WAIT_FOR_AGENT_AUTH", { builder -> builder
                        .guard({ ctx ->
                            def loginState = ctx.getGameModel()?.getLoginState()
                            return isAuthenticated(loginState) || isLoggedIn(ctx)
                        })
                        .action({ ctx -> })
                        .nextState("WAIT_FOR_CHARACTER")
                        .targetState("CHECK_AGENT_AUTH_TIMEOUT")
                })
                .state("CHECK_AGENT_AUTH_TIMEOUT", { builder -> builder
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
                            if (loginState?.getPhase() != LoginState.Phase.FAILED && loginState != null) {
                                loginState.setFailureReason("Agent authentication timeout")
                            }
                            log.warn("Agent authentication timed out after {} ms (phase={})",
                                    AGENT_AUTH_TIMEOUT_MS, loginState?.getPhase())
                        })
                        .nextState("RETRY_DELAY")
                        .targetState("WAIT_FOR_AGENT_AUTH_RECHECK")
                })
                .delayState("WAIT_FOR_AGENT_AUTH_RECHECK", { builder -> builder
                        .delay(500)
                        .nextState("WAIT_FOR_AGENT_AUTH")
                })
                .state("WAIT_FOR_CHARACTER", { builder -> builder
                        .guard({ ctx ->
                            if (isLoggedIn(ctx)) {
                                return true
                            }
                            def loginState = ctx.getGameModel()?.getLoginState()
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            if (isAuthenticated(loginState)) {
                                def availableCharacters = loginState?.getAvailableCharacterNames() ?: []
                                if (!availableCharacters.isEmpty() && isBlank(settings?.selectedCharacter)) {
                                    emitEnginePhase(ctx, PHASE_MISSING_CHARACTER_SELECTION, "MissingRequirement", null)
                                    return false
                                }
                            }
                            return false
                        })
                        .action({ ctx ->
                            resetRetry(ctx)
                            ctx.getPersistentData().remove(KEY_LOGIN_HALTED_CREDENTIAL)
                            ctx.getPersistentData().remove("explicitConnectRequested")
                            emitEnginePhase(ctx, LoginState.Phase.AUTHENTICATED.name(), "Authenticated", null)
                            log.info("Login/authentication successful")
                        })
                        .nextState(null)
                        .targetState("CHECK_CHARACTER_WAIT_TIMEOUT")
                })
                .state("CHECK_CHARACTER_WAIT_TIMEOUT", { builder -> builder
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
                            if (loginState != null) {
                                loginState.setFailureReason("Character selection or in-game timeout")
                            }
                            log.warn("Post-auth wait timed out after {} ms (phase={})", AGENT_AUTH_TIMEOUT_MS,
                                    ctx.getGameModel()?.getLoginState()?.getPhase())
                        })
                        .nextState("RETRY_DELAY")
                        .targetState("WAIT_FOR_CHARACTER_RECHECK")
                })
                .delayState("WAIT_FOR_CHARACTER_RECHECK", { builder -> builder
                        .delay(500)
                        .nextState("WAIT_FOR_CHARACTER")
                })
                .state("RETRY_DELAY", { builder -> builder
                        .guard({ ctx -> true })
                        .action({ ctx ->
                            def settings = resolveRuntimeSettings(ctx, settingsProvider, true)
                            def loginState = ctx.getGameModel()?.getLoginState()
                            String failureClass = classifyFailure(loginState, ctx)
                            log.info("Failure classified as {}", failureClass)
                            String haltSignature = buildHaltSignature(ctx, loginState, failureClass)
                            String lastHaltSignature = String.valueOf(ctx.getPersistentData().getOrDefault(KEY_LOGIN_LAST_HALT_SIGNATURE, ""))
                            boolean haltAlreadyLogged = haltSignature == lastHaltSignature

                            if (FAILURE_MANUAL_VERIFICATION.equals(failureClass)) {
                                emitEnginePhase(ctx, "MANUAL_VERIFICATION_REQUIRED", "ManualVerification", String.valueOf(loginState?.getFailureReason() ?: ""))
                                if (!haltAlreadyLogged) {
                                    log.warn("Login paused: manual verification required ({})", loginState?.getFailureReason())
                                    ctx.getPersistentData().put(KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature)
                                }
                                return
                            }
                            if (FAILURE_CREDENTIAL.equals(failureClass)) {
                                ctx.getPersistentData().put(KEY_LOGIN_HALTED_CREDENTIAL, true)
                                emitEnginePhase(ctx, LoginState.Phase.FAILED.name(), "CredentialFailure", String.valueOf(loginState?.getFailureReason() ?: ""))
                                if (!haltAlreadyLogged) {
                                    log.warn("Infinite retry disabled for credential failures")
                                    log.error("Login halted: credential failure ({})", loginState?.getFailureReason())
                                    ctx.getPersistentData().put(KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature)
                                }
                                return
                            }
                            if (FAILURE_AGENT_TIMEOUT.equals(failureClass)) {
                                emitEnginePhase(ctx, PHASE_WAITING_FOR_AGENTS_TIMEOUT, "WaitingForAgentsTimeout",
                                        String.valueOf(loginState?.getFailureReason() ?: "Agent list timeout"))
                            }
                            if (settings != null && !toBool(settings.autoReconnect, true)) {
                                emitEnginePhase(ctx, "RETRY_DISABLED", "RetryDisabled", null)
                                if (!haltAlreadyLogged) {
                                    log.warn("Login retries halted: auto reconnect disabled in settings")
                                    ctx.getPersistentData().put(KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature)
                                }
                                return
                            }

                            if (settings != null && !canRetry(ctx, settings, failureClass)) {
                                emitEnginePhase(ctx, "RETRY_LIMIT_REACHED", "RetryLimitReached", null)
                                if (!haltAlreadyLogged) {
                                    log.warn("Login retries halted: max retry attempts reached ({})", toInt(settings.maxRetryAttempts, 0))
                                    ctx.getPersistentData().put(KEY_LOGIN_LAST_HALT_SIGNATURE, haltSignature)
                                }
                                return
                            }
                            ctx.getPersistentData().remove(KEY_LOGIN_LAST_HALT_SIGNATURE)

                            long requestedDelayMs = settings != null
                                    ? Math.max(0L, (long) toInt(settings.retryBaseDelayMs, 5000))
                                    : RETRY_DELAYS_MS.get(0)
                            long delayMs = nextRetryDelayMs(ctx, settings)
                            boolean infiniteRequested = settings != null && toBool(settings.infiniteRetryMode, true)
                            boolean infiniteEffective = (FAILURE_NETWORK.equals(failureClass) || FAILURE_AGENT_TIMEOUT.equals(failureClass)) && infiniteRequested
                            String phase = String.valueOf(loginState?.getPhase() ?: "DISCONNECTED")
                            String host = String.valueOf(ctx.getPersistentData().get("gatewayHost") ?: "")
                            String port = String.valueOf(ctx.getPersistentData().get("gatewayPort") ?: "")
                            log.info("Retry policy applied: requestedDelay={} effectiveDelay={} infinite={}",
                                    requestedDelayMs, delayMs, infiniteEffective)
                            log.info("RETRY_DELAY context machine={} phase={} serverConnected={} gateway={}:{}",
                                    ctx.getMachineName(), phase, ctx.getDispatcher().isServerConnected(), host, port)
                            log.warn("Login retry in {} ms (reason: {})", delayMs, loginState?.getFailureReason() ?: "unknown")
                            emitEnginePhase(ctx, "RETRY_DELAY", "RetryDelay", String.valueOf(loginState?.getFailureReason() ?: ""))
                            try {
                                Thread.sleep(delayMs)
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt()
                            }
                        })
                        .delay(0)
                        .nextState(null)
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

    private short parseAgentId(String targetAgent) {
        if (!targetAgent) return (short) 0
        try { return Short.parseShort(targetAgent) }
        catch (NumberFormatException e) { log.warn("Invalid agent ID format: {}", targetAgent); return (short) 0 }
    }

    private Map parseGateway(String raw) {
        if (!raw) {
            log.warn("No target gateway configured")
            return null
        }
        def value = raw.trim()
        if (value.isEmpty()) {
            log.warn("Empty target gateway configured")
            return null
        }

        if (value.contains(":")) {
            def parts = value.split(":", 2)
            if (parts[0]?.trim()) {
                try {
                    return [host: parts[0].trim(), port: Integer.parseInt(parts[1].trim())]
                } catch (Exception e) {
                    log.warn("Invalid target gateway format '{}', expected host:port", raw)
                    return null
                }
            }
        }

        return [host: value, port: 15779]
    }

    private boolean hasAgents(def loginState) {
        try {
            return loginState != null && loginState.getAgentList() != null && !loginState.getAgentList().isEmpty()
        } catch (Exception e) {
            return false
        }
    }

    /** True when we can send 0x6102: agent list present plus username, password, passcode, and target shard id. */
    private boolean loginPrereqsForGateway(def settings, def loginState) {
        if (settings == null) return false
        if (!hasAgents(loginState)) return false
        if (isBlank(settings.username) || isBlank(settings.password) || isBlank(settings.passcode)) return false
        if (isBlank(settings.targetAgent)) return false
        return true
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

    private long nextRetryDelayMs(def ctx, def settings) {
        def attemptRaw = ctx.getPersistentData().getOrDefault("loginRetryAttempt", 0)
        int attempt = (attemptRaw instanceof Number) ? attemptRaw.intValue() : 0
        int nextAttempt = attempt + 1
        ctx.getPersistentData().put("loginRetryAttempt", nextAttempt)

        if (settings == null) {
            int index = Math.max(0, Math.min(nextAttempt - 1, RETRY_DELAYS_MS.size() - 1))
            return clampDelay(RETRY_DELAYS_MS.get(index))
        }

        long baseDelay = clampDelay(Math.max(0L, (long) toInt(settings.retryBaseDelayMs, 5000)))
        long maxDelay = clampDelay(Math.max(baseDelay, (long) toInt(settings.retryMaxDelayMs, 60000)))
        long computed = baseDelay
        if (nextAttempt > 1) {
            computed = baseDelay * (1L << Math.min(nextAttempt - 1, 20))
        }
        return clampDelay(Math.min(computed, maxDelay))
    }

    private boolean canRetry(def ctx, def settings, String failureClass) {
        if (settings == null) return true
        boolean infiniteRequested = toBool(settings.infiniteRetryMode, true)
        if (infiniteRequested && (FAILURE_NETWORK.equals(failureClass) || FAILURE_AGENT_TIMEOUT.equals(failureClass))) return true
        if (infiniteRequested && !FAILURE_NETWORK.equals(failureClass) && !FAILURE_AGENT_TIMEOUT.equals(failureClass)) return false

        int maxAttempts = toInt(settings.maxRetryAttempts, 0)
        if (maxAttempts <= 0) {
            return true
        }
        def attemptRaw = ctx.getPersistentData().getOrDefault("loginRetryAttempt", 0)
        int attempt = (attemptRaw instanceof Number) ? attemptRaw.intValue() : 0
        return attempt < maxAttempts
    }

    private Map resolveRuntimeSettings(def ctx, def settingsProvider, boolean allowInitialize) {
        boolean explicitConnect = Boolean.TRUE.equals(ctx.getPersistentData().get("explicitConnectRequested"))
        def existing = ctx.getPersistentData().get("runtimeLoginSettings")
        if (existing instanceof Map && !explicitConnect) {
            return existing
        }
        if (!allowInitialize) {
            return null
        }

        def raw = settingsProvider?.get()
        if (raw == null) {
            return null
        }

        def snapshot = [
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
                infiniteRetryMode: raw.isInfiniteRetryMode()
        ]
        ctx.getPersistentData().put("runtimeLoginSettings", snapshot)
        if (explicitConnect) {
            // Force re-emission of the current missing/transition phase after an explicit Connect click.
            ctx.getPersistentData().remove("uiLoginPhase")
            ctx.getPersistentData().remove(KEY_LOGIN_LAST_HALT_SIGNATURE)
            log.info("Refreshed runtime login settings for explicit connect on machine {}", ctx.getMachineName())
        }
        return snapshot
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

    private long clampDelay(long value) {
        return Math.max(MIN_RETRY_DELAY_MS, Math.min(value, MAX_RETRY_DELAY_MS))
    }

    private String classifyFailure(def loginState, def ctx) {
        if (isAgentWaitTimedOut(ctx)) {
            return FAILURE_AGENT_TIMEOUT
        }
        if (requiresManualVerification(loginState)) {
            return FAILURE_MANUAL_VERIFICATION
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
        if (reason.contains("password")
                || reason.contains("invalid account")
                || reason.contains("invalid credentials")
                || reason.contains("auth failed")
                || reason.contains("rejected")
                || reason.contains("denied")) {
            return FAILURE_CREDENTIAL
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

        return FAILURE_NETWORK
    }

    private void resetRetry(def ctx) {
        ctx.getPersistentData().put("loginRetryAttempt", 0)
        ctx.getPersistentData().put(KEY_AGENT_WAIT_TIMED_OUT, false)
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

    private void emitEnginePhase(def ctx, String phase, String transition, String reason) {
        try {
            def state = ctx?.getGameModel()?.getLoginState()
            setLoginStatePhase(state, phase)
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

            String previous = String.valueOf(ctx.getPersistentData().getOrDefault("uiLoginPhase", ""))
            if (phase == previous) {
                return
            }
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
            eventAdmin.postEvent(new Event("sokybot/network/${osgiEventTopicSeg(machineId)}/EnginePhase", props))
        } catch (Exception ignored) {
            // Never fail cycle execution on telemetry/event updates.
        }
    }

    private void setLoginStatePhase(def loginState, String phase) {
        if (loginState == null || phase == null) return
        try {
            switch (phase) {
                case PHASE_MISSING_GATEWAY:
                    loginState.setPhase(LoginState.Phase.MISSING_GATEWAY); return
                case PHASE_MISSING_CREDENTIALS:
                    loginState.setPhase(LoginState.Phase.MISSING_CREDENTIALS); return
                case PHASE_MISSING_AGENT_SERVER:
                    loginState.setPhase(LoginState.Phase.MISSING_AGENT_SERVER); return
                case PHASE_MISSING_CHARACTER_SELECTION:
                    loginState.setPhase(LoginState.Phase.MISSING_CHARACTER_SELECTION); return
                case LoginState.Phase.LOGIN_SENT.name():
                    loginState.setPhase(LoginState.Phase.LOGIN_SENT); return
                case LoginState.Phase.AUTHENTICATED.name():
                    loginState.setPhase(LoginState.Phase.AUTHENTICATED); return
                case PHASE_WAITING_FOR_AGENTS:
                    loginState.setPhase(LoginState.Phase.WAITING_FOR_AGENTS); return
                case PHASE_WAITING_FOR_AGENTS_TIMEOUT:
                    loginState.setPhase(LoginState.Phase.WAITING_FOR_AGENTS_TIMEOUT); return
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
}

new Login()
