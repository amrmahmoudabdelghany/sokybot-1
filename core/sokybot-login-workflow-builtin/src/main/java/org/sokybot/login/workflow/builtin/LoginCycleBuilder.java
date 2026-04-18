package org.sokybot.login.workflow.builtin;

import java.util.HashMap;
import java.util.Map;

import org.sokybot.engine.api.login.LoginSettingsSnapshot;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.engine.api.workflow.IGuard;
import org.sokybot.engine.core.workflow.builder.CycleDefinitionBuilder;
import org.sokybot.gamemodel.LoginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assembles the {@code login-cycle} {@link ICycleDefinition} (ported from {@code Login.groovy}).
 */
final class LoginCycleBuilder {

    private static final Logger log = LoggerFactory.getLogger(LoginCycleBuilder.class);

    private final LoginWorkflowSupport w;

    LoginCycleBuilder(LoginWorkflowSupport w) {
        this.w = w;
    }

    ICycleDefinition build() {
        return new CycleDefinitionBuilder()
                .name(LoginCycleKeys.CYCLE_NAME)
                .priority(200)
                .entryState(LoginCycleKeys.STATE_CHECK_CONNECTION)
                .entryGuard((IGuard) ctx -> {
                    boolean explicitConnect = Boolean.TRUE.equals(ctx.getPersistentData().get("explicitConnectRequested"));
                    if (explicitConnect) {
                        w.resolveRuntimeSettings(ctx, true);
                    }
                    LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                    LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                    log.info(
                            "Login-cycle entryGuard: machine={}, explicitConnect={}, hasSettings={}, targetGateway='{}'",
                            ctx.getMachineName(), explicitConnect, settings != null,
                            settings != null ? settings.getTargetGateway() : "");
                    if (w.isHalted(ctx, loginState) && !explicitConnect) {
                        log.info("Login-cycle entryGuard: halted until explicit connect machine={}", ctx.getMachineName());
                        return false;
                    }
                    boolean haltedByCredential = Boolean.TRUE.equals(ctx.getPersistentData().get(LoginCycleKeys.KEY_LOGIN_HALTED_CREDENTIAL));
                    boolean waitingForExplicitResume = Boolean.TRUE.equals(ctx.getPersistentData().get(LoginCycleKeys.KEY_USER_RESUME_REQUIRED));
                    if (waitingForExplicitResume && !explicitConnect) {
                        return false;
                    }
                    if (w.isPasscodeInputRequired(loginState, ctx) && !w.isInteractiveWaitTimedOut(ctx)) {
                        Map<String, Object> ex = new HashMap<>();
                        ex.put("interactive", true);
                        ex.put("waitUntil", w.readInteractiveWaitUntil(ctx));
                        w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_WAITING_FOR_PASSCODE, "WaitingForPasscode",
                                String.valueOf(loginState != null ? loginState.getFailureReason() : ""), ex);
                        return false;
                    }
                    if (haltedByCredential && !explicitConnect) {
                        return false;
                    }
                    boolean result = settings != null
                            && !w.requiresManualVerification(loginState)
                            && !w.isAuthenticated(loginState)
                            && !w.isLoggedIn(ctx);
                    if (!result) {
                        w.logInfoCtx(ctx, "Login entry guard FAILED: settings={} autoLogin={} explicitConnect={} requiresVerification={} isAuthenticated={} isLoggedIn={} haltedByCredential={}",
                                settings != null, settings != null && settings.isAutoLogin(), explicitConnect,
                                w.requiresManualVerification(loginState), w.isAuthenticated(loginState), w.isLoggedIn(ctx), haltedByCredential);
                    }
                    return result;
                })
                .state(LoginCycleKeys.STATE_CHECK_CONNECTION, b -> b
                        .guard(ctx -> {
                            if (ctx.getDispatcher().isServerConnected()) {
                                return false;
                            }
                            LoginState.Phase phase = ctx.getGameModel() != null && ctx.getGameModel().getLoginState() != null
                                    ? ctx.getGameModel().getLoginState().getPhase() : null;
                            return phase != LoginState.Phase.LOGIN_SUCCESS && phase != LoginState.Phase.REDIRECTING
                                    && phase != LoginState.Phase.AGENT_CONNECTED;
                        })
                        .action(ctx -> {
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            LoginWorkflowSupport.HostPort gateway = w.resolveGatewayForAttempt(ctx,
                                    settings != null ? String.valueOf(settings.getTargetGateway()) : "");
                            if (gateway != null) {
                                ctx.getPersistentData().put("gatewayHost", gateway.host);
                                ctx.getPersistentData().put("gatewayPort", gateway.port);
                                ctx.getPersistentData().put("loginLastGatewayRaw", String.valueOf(settings != null ? settings.getTargetGateway() : ""));
                                w.logInfoCtx(ctx, "CHECK_CONNECTION resolved gateway -> {}:{} (raw='{}')",
                                        gateway.host, gateway.port, String.valueOf(settings != null ? settings.getTargetGateway() : ""));
                            } else {
                                ctx.getPersistentData().remove("gatewayHost");
                                ctx.getPersistentData().remove("gatewayPort");
                                w.logWarnCtx(ctx, "CHECK_CONNECTION gateway parse failed (raw='{}'), skipping connect",
                                        String.valueOf(settings != null ? settings.getTargetGateway() : ""));
                            }
                        })
                        .nextState(LoginCycleKeys.STATE_CHECK_CONNECTION_ROUTE)
                        .targetState(LoginCycleKeys.STATE_POST_CHECK_CONNECTION))
                .conditionalState(LoginCycleKeys.STATE_CHECK_CONNECTION_ROUTE, cb -> cb
                        .when(ctx -> {
                            Object h = ctx.getPersistentData().get("gatewayHost");
                            return h == null || String.valueOf(h).trim().isEmpty();
                        }, LoginCycleKeys.STATE_PARK_MISSING_PREREQS)
                        .when(ctx -> {
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            boolean explicitConnect = Boolean.TRUE.equals(ctx.getPersistentData().get("explicitConnectRequested"));
                            return settings != null && !settings.isAutoLogin() && !explicitConnect;
                        }, LoginCycleKeys.STATE_PARK_MANUAL_CONNECT)
                        .defaultTo(LoginCycleKeys.STATE_CONNECT_TO_GATEWAY))
                .exitState(LoginCycleKeys.STATE_PARK_MISSING_PREREQS, eb -> eb
                        .beforeExit(ctx -> w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_MISSING_GATEWAY, "MissingRequirement", null)))
                .exitState(LoginCycleKeys.STATE_PARK_MANUAL_CONNECT, eb -> eb
                        .beforeExit(ctx -> w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_PENDING_MANUAL_CONNECT, "ReadyToConnect", null)))
                .conditionalState(LoginCycleKeys.STATE_POST_CHECK_CONNECTION, cb -> cb
                        .when(ctx -> {
                            LoginState.Phase phase = ctx.getGameModel() != null && ctx.getGameModel().getLoginState() != null
                                    ? ctx.getGameModel().getLoginState().getPhase() : null;
                            boolean midRedirect = phase == LoginState.Phase.LOGIN_SUCCESS || phase == LoginState.Phase.REDIRECTING
                                    || phase == LoginState.Phase.AGENT_CONNECTED;
                            return midRedirect && !ctx.getDispatcher().isServerConnected();
                        }, LoginCycleKeys.STATE_WAIT_FOR_LOGIN_RESPONSE)
                        .when(ctx -> ctx.getDispatcher().isServerConnected(), LoginCycleKeys.STATE_CONNECTED_RESUME)
                        .defaultTo(LoginCycleKeys.STATE_RETRY_DELAY))
                .conditionalState(LoginCycleKeys.STATE_CONNECTED_RESUME, cb -> cb
                        .when(ctx -> {
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            LoginState ls = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            return w.loginPrereqsForGateway(settings, ls, ctx);
                        }, LoginCycleKeys.STATE_PAUSE_BEFORE_GATEWAY_LOGIN)
                        .when(ctx -> w.hasAgents(ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null),
                                LoginCycleKeys.STATE_PARK_MISSING_LOGIN)
                        .defaultTo(LoginCycleKeys.STATE_RESEND_AGENT_LIST_REQUEST))
                .state(LoginCycleKeys.STATE_RESEND_AGENT_LIST_REQUEST, b -> b
                        .guard(ctx -> true)
                        .action(ctx -> {
                            w.resetRetry(ctx);
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_WAIT_DEADLINE_MS,
                                    System.currentTimeMillis() + w.effectiveAgentWaitTimeoutMs(settings));
                            ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_WAIT_TIMED_OUT, false);
                            w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_WAITING_FOR_AGENTS, "WaitingForAgents", null);
                            w.logInfoCtx(ctx, "Already connected to gateway; (re-)requesting agent list");
                            if (!w.requestAgentList(ctx, settings, true)) {
                                ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_WAIT_DEADLINE_MS, System.currentTimeMillis() - 1L);
                            }
                        })
                        .nextState(LoginCycleKeys.STATE_WAIT_FOR_AGENTS)
                        .targetState(LoginCycleKeys.STATE_RETRY_DELAY))
                .state(LoginCycleKeys.STATE_PARK_MISSING_LOGIN, b -> b
                        .guard(ctx -> true)
                        .action(ctx -> {
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            if (settings == null) {
                                return;
                            }
                            String preflight = w.evaluatePreflightStatus(settings, loginState);
                            if ("MISSING_CREDENTIALS".equals(preflight)) {
                                ctx.getPersistentData().put(LoginCycleKeys.KEY_USER_RESUME_REQUIRED, true);
                                w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_MISSING_CREDENTIALS, "MissingRequirement", null);
                            } else if ("MISSING_AGENT_SERVER".equals(preflight)) {
                                ctx.getPersistentData().put(LoginCycleKeys.KEY_USER_RESUME_REQUIRED, true);
                                w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_MISSING_AGENT_SERVER, "MissingRequirement", null);
                            } else if ("SERVER_INSPECTION".equals(preflight)) {
                                w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_SERVER_INSPECTION, "ServerInspection",
                                        "Target server is under inspection. Retrying automatically.");
                            }
                            w.logInfoCtx(ctx, "Gateway session holds agent list; waiting for login prerequisites");
                        })
                        .nextState((String) null)
                        .targetState(LoginCycleKeys.STATE_RETRY_DELAY))
                .state(LoginCycleKeys.STATE_CONNECT_TO_GATEWAY, b -> b
                        .guard(ctx -> ctx.getPersistentData().get("gatewayHost") != null)
                        .action(ctx -> {
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            if (settings != null) {
                                w.beginAttemptSnapshot(ctx, settings);
                            }
                            String host = w.normalizeHostForSocket((String) ctx.getPersistentData().get("gatewayHost"));
                            Object portObj = ctx.getPersistentData().get("gatewayPort");
                            int port = portObj instanceof Number ? ((Number) portObj).intValue() : 0;
                            w.emitEnginePhase(ctx, "CONNECTING_GATEWAY", "Connecting", null);
                            w.logInfoCtx(ctx, "CONNECT_TO_GATEWAY attempting -> {}:{} (alreadyConnected={})",
                                    host, port, ctx.getDispatcher().isServerConnected());
                            w.applyGatewayHandshakeHintsToProxy(ctx, settings);
                            ctx.getDispatcher().setClientlessMode(true);
                            ctx.getDispatcher().connect(host, port);
                            w.logInfoCtx(ctx, "CONNECT_TO_GATEWAY result connected={}", ctx.getDispatcher().isServerConnected());
                        })
                        .nextState(LoginCycleKeys.STATE_WAIT_FOR_CONNECTION)
                        .targetState(LoginCycleKeys.STATE_RETRY_DELAY))
                .state(LoginCycleKeys.STATE_WAIT_FOR_CONNECTION, b -> b
                        .guard(ctx -> ctx.getDispatcher().isServerConnected())
                        .action(ctx -> {
                            w.resetRetry(ctx);
                            w.markGatewayConnectSuccess(ctx);
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_LAST_GATEWAY_LOGIN_REQUEST_MS);
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_WAIT_DEADLINE_MS,
                                    System.currentTimeMillis() + w.effectiveAgentWaitTimeoutMs(settings));
                            ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_WAIT_TIMED_OUT, false);
                            w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_WAITING_FOR_AGENTS, "WaitingForAgents", null);
                            w.logInfoCtx(ctx, "Gateway connected, requesting agent list");
                            if (!w.requestAgentList(ctx, settings, true)) {
                                ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_WAIT_DEADLINE_MS, System.currentTimeMillis() - 1L);
                            }
                        })
                        .nextState(LoginCycleKeys.STATE_WAIT_FOR_AGENTS)
                        .targetState(LoginCycleKeys.STATE_RETRY_DELAY))
                .state(LoginCycleKeys.STATE_WAIT_FOR_AGENTS, b -> b
                        .guard(ctx -> {
                            if (Boolean.TRUE.equals(ctx.getPersistentData().get(LoginCycleKeys.KEY_LOGIN_HALTED_CREDENTIAL))) {
                                return false;
                            }
                            return w.hasAgentListReceived(ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null);
                        })
                        .action(ctx -> {
                            ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_WAIT_TIMED_OUT, false);
                            ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_LIST_CACHE_AT_MS, System.currentTimeMillis());
                            w.emitEnginePhase(ctx, LoginState.Phase.AGENTS_RECEIVED.name(), "AgentsReceived", null);
                            w.logInfoCtx(ctx, "Agent list received, sending login request");
                        })
                        .nextState(LoginCycleKeys.STATE_PAUSE_BEFORE_GATEWAY_LOGIN)
                        .targetState(LoginCycleKeys.STATE_CHECK_AGENT_WAIT_TIMEOUT))
                .delayState(LoginCycleKeys.STATE_PAUSE_BEFORE_GATEWAY_LOGIN, b -> b
                        .delay(0)
                        .minDelay(0)
                        .delayAction(ctx -> {
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            long ms = w.effectiveGatewayLoginPauseAfterAgentListMs(settings);
                            if (ms > 0L) {
                                java.util.Map<String, Object> ex = new java.util.HashMap<>();
                                ex.put("pauseMs", ms);
                                w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_GATEWAY_LOGIN_PAUSE, "GatewayLoginPause", null, ex);
                            }
                        })
                        .nextState(LoginCycleKeys.STATE_SEND_LOGIN_REQUEST))
                .state(LoginCycleKeys.STATE_CHECK_AGENT_WAIT_TIMEOUT, b -> b
                        .guard(w::isAgentWaitTimedOut)
                        .action(ctx -> {
                            ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_WAIT_TIMED_OUT, true);
                            w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_WAITING_FOR_AGENTS_TIMEOUT, "WaitingForAgentsTimeout", null);
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            if (loginState != null) {
                                loginState.setFailureReason("Agent list timeout");
                            }
                            w.logWarnCtx(ctx, "Agent list wait timed out after {} ms; entering retry flow", w.effectiveAgentWaitTimeoutMs(settings));
                        })
                        .nextState(LoginCycleKeys.STATE_RETRY_DELAY)
                        .targetState(LoginCycleKeys.STATE_WAIT_FOR_AGENTS_RECHECK))
                .delayState(LoginCycleKeys.STATE_WAIT_FOR_AGENTS_RECHECK, b -> b.delay(500).nextState(LoginCycleKeys.STATE_WAIT_FOR_AGENTS))
                .state(LoginCycleKeys.STATE_SEND_LOGIN_REQUEST, b -> b
                        .guard(ctx -> {
                            LoginSettingsSnapshot settings = w.currentAttemptSettings(ctx);
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            if (settings == null) {
                                return false;
                            }
                            if (Boolean.TRUE.equals(ctx.getPersistentData().get(LoginCycleKeys.KEY_LOGIN_HALTED_CREDENTIAL))) {
                                return false;
                            }
                            if (Boolean.TRUE.equals(ctx.getPersistentData().get(LoginCycleKeys.KEY_LOGIN_IN_PROGRESS))) {
                                return false;
                            }
                            try {
                                if (loginState != null && loginState.getPhase() == LoginState.Phase.LOGIN_SENT) {
                                    return false;
                                }
                            } catch (Exception ignored) {
                            }
                            String preflight = w.evaluatePreflightStatus(settings, loginState);
                            if ("MISSING_CREDENTIALS".equals(preflight)) {
                                ctx.getPersistentData().put(LoginCycleKeys.KEY_USER_RESUME_REQUIRED, true);
                                w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_MISSING_CREDENTIALS, "MissingRequirement", null);
                                return false;
                            }
                            if ("MISSING_AGENT_SERVER".equals(preflight)) {
                                ctx.getPersistentData().put(LoginCycleKeys.KEY_USER_RESUME_REQUIRED, true);
                                w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_MISSING_AGENT_SERVER, "MissingRequirement", null);
                                return false;
                            }
                            if ("SERVER_INSPECTION".equals(preflight)) {
                                w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_SERVER_INSPECTION, "ServerInspection",
                                        "Target server is under inspection. Retrying automatically.");
                                return false;
                            }
                            return true;
                        })
                        .action(ctx -> {
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_USER_RESUME_REQUIRED);
                            LoginSettingsSnapshot settings = w.currentAttemptSettings(ctx);
                            if (settings == null) {
                                return;
                            }
                            String username = String.valueOf(settings.getUsername());
                            String password = String.valueOf(settings.getPassword());
                            synchronized (ctx) {
                                if (Boolean.TRUE.equals(ctx.getPersistentData().get(LoginCycleKeys.KEY_LOGIN_IN_PROGRESS))) {
                                    w.logWarnCtx(ctx, "Login request ignored: already in progress");
                                    return;
                                }
                                long gateNow = System.currentTimeMillis();
                                if (!w.allowGatewayLoginRequestNow(ctx, settings, gateNow)) {
                                    w.logWarnCtx(ctx, "Gateway 0x6102 deferred: min interval {} ms (machine={}, serverConnected={})",
                                            w.effectiveGatewayLoginMinIntervalMs(settings), ctx.getMachineName(), w.safeIsServerConnected(ctx));
                                    return;
                                }
                                ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_IN_PROGRESS, true);
                                ctx.getPersistentData().put(LoginCycleKeys.KEY_LAST_GATEWAY_LOGIN_REQUEST_MS, gateNow);
                                try {
                                    byte locale = w.effectiveGatewayLocale(settings);
                                    short agentId = w.parseAgentId(String.valueOf(settings.getTargetAgent()));
                                    String charsetName = String.valueOf(settings.getLoginCharset());
                                    w.logInfoCtx(ctx, "Sending gateway 0x6102 for user: {} (locale=0x{}, agentId={}, charset={}, serverConnected={})",
                                            LoginWorkflowSupport.redactForLog(username), String.format("%02X", locale & 0xFF),
                                            (int) agentId & 0xFFFF, charsetName, w.safeIsServerConnected(ctx));
                                    LoginState ls0 = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                                    if (ls0 != null) {
                                        ls0.setPhase(LoginState.Phase.LOGIN_SENT);
                                    }
                                    w.emitEnginePhase(ctx, LoginState.Phase.LOGIN_SENT.name(), "LoginSent", null);
                                    long now = System.currentTimeMillis();
                                    ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_RESPONSE_DEADLINE_MS,
                                            now + w.effectiveLoginResponseTimeoutMs(settings));
                                    ctx.getPersistentData().put(LoginCycleKeys.KEY_AGENT_AUTH_DEADLINE_MS,
                                            now + w.effectiveAgentAuthTimeoutMs(settings));
                                    w.sendLoginRequest(ctx, locale, username, password, agentId, charsetName);
                                } catch (Throwable t) {
                                    ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_IN_PROGRESS, false);
                                    ctx.getPersistentData().remove(LoginCycleKeys.KEY_LAST_GATEWAY_LOGIN_REQUEST_MS);
                                    if (t instanceof Error && !(t instanceof Exception)) {
                                        throw (Error) t;
                                    }
                                    if (t instanceof RuntimeException) {
                                        throw (RuntimeException) t;
                                    }
                                    if (t instanceof Exception) {
                                        throw new RuntimeException(t);
                                    }
                                    throw new RuntimeException(t);
                                }
                            }
                        })
                        .nextState(LoginCycleKeys.STATE_WAIT_FOR_LOGIN_RESPONSE)
                        .targetState(LoginCycleKeys.STATE_RETRY_DELAY))
                .state(LoginCycleKeys.STATE_WAIT_FOR_LOGIN_RESPONSE, b -> b
                        .guard(ctx -> {
                            LoginWorkflowSupport.FlowSnapshot s = w.snapshot(ctx);
                            LoginState.Phase phase = s.phase;
                            if (phase == LoginState.Phase.LOGIN_SUCCESS || phase == LoginState.Phase.REDIRECTING
                                    || phase == LoginState.Phase.AGENT_CONNECTED
                                    || phase == LoginState.Phase.AUTHENTICATED) {
                                return true;
                            }
                            return s.loggedIn;
                        })
                        .action(ctx -> {
                            LoginWorkflowSupport.FlowSnapshot s = w.snapshot(ctx);
                            w.logInfoCtx(ctx, "Gateway login response received (phase={})", s.phase);
                        })
                        .nextState(LoginCycleKeys.STATE_WAIT_FOR_AGENT_SERVER_CONNECTION)
                        .targetState(LoginCycleKeys.STATE_SUBMIT_GATEWAY_IMAGE_CODE))
                .state(LoginCycleKeys.STATE_SUBMIT_GATEWAY_IMAGE_CODE, b -> b
                        .guard(ctx -> {
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            return loginState != null && loginState.getPhase() == LoginState.Phase.WAIT_FOR_CAPTCHA && w.safeIsServerConnected(ctx);
                        })
                        .action(ctx -> {
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, false);
                            String answer = settings != null ? String.valueOf(settings.getPasscode()).trim() : "";
                            java.util.Map<String, Object> ex = new java.util.HashMap<>();
                            ex.put("interactive", true);
                            ex.put("gatewayImageCode", true);
                            w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_WAIT_FOR_CAPTCHA, "GatewayImageCode",
                                    "Gateway image code challenge; answer from login passcode field (UTF-16, may be empty)", ex);
                            w.sendGatewayImageCodeAnswer(ctx, answer);
                            LoginState ls = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            if (ls != null) {
                                ls.setPhase(LoginState.Phase.LOGIN_SENT);
                                ls.setFailureReason(null);
                            }
                            long now = System.currentTimeMillis();
                            LoginSettingsSnapshot s = w.resolveRuntimeSettings(ctx, true);
                            ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_RESPONSE_DEADLINE_MS,
                                    now + w.effectiveLoginResponseTimeoutMs(s));
                            w.logInfoCtx(ctx, "Sent gateway 0x6323 image code answer (chars={})", answer.length());
                        })
                        .nextState(LoginCycleKeys.STATE_WAIT_FOR_LOGIN_RECHECK)
                        .targetState(LoginCycleKeys.STATE_CHECK_LOGIN_FAILED_IMMEDIATE))
                .state(LoginCycleKeys.STATE_WAIT_FOR_AGENT_SERVER_CONNECTION, b -> b
                        .guard(ctx -> {
                            LoginWorkflowSupport.FlowSnapshot s = w.snapshot(ctx);
                            LoginState.Phase phase = s.phase;
                            return phase == LoginState.Phase.AGENT_CONNECTED
                                    || phase == LoginState.Phase.AUTHENTICATED
                                    || s.loggedIn;
                        })
                        .action(ctx -> { })
                        .nextState(LoginCycleKeys.STATE_WAIT_FOR_AGENT_AUTH)
                        .targetState(LoginCycleKeys.STATE_CHECK_AGENT_SERVER_CONNECTION_FAILURE))
                .state(LoginCycleKeys.STATE_CHECK_AGENT_SERVER_CONNECTION_FAILURE, b -> b
                        .guard(ctx -> {
                            LoginWorkflowSupport.FlowSnapshot s = w.snapshot(ctx);
                            if (s.phase == LoginState.Phase.FAILED) {
                                return true;
                            }
                            if (s.loggedIn || s.phase == LoginState.Phase.AGENT_CONNECTED || s.phase == LoginState.Phase.AUTHENTICATED) {
                                return false;
                            }
                            if (w.isAgentAuthTimedOut(ctx)) {
                                return true;
                            }
                            return w.readLatestNetworkFailure(ctx) != null;
                        })
                        .action(ctx -> {
                            LoginWorkflowSupport.FlowSnapshot s = w.snapshot(ctx);
                            LoginState loginState = s.loginState;
                            java.util.Map<String, Object> ev = w.readLatestNetworkFailure(ctx);
                            if (loginState != null && ev != null) {
                                loginState.setFailureReason(String.valueOf(ev.getOrDefault("reason", "Agent connection failed")));
                            } else if (loginState != null && w.isAgentAuthTimedOut(ctx) && loginState.getPhase() != LoginState.Phase.FAILED) {
                                loginState.setFailureReason("Agent server connection timeout");
                            }
                        })
                        .nextState(LoginCycleKeys.STATE_RETRY_DELAY)
                        .targetState(LoginCycleKeys.STATE_WAIT_FOR_AGENT_SERVER_CONNECTION_RECHECK))
                .delayState(LoginCycleKeys.STATE_WAIT_FOR_AGENT_SERVER_CONNECTION_RECHECK, b -> b
                        .delay(500).nextState(LoginCycleKeys.STATE_WAIT_FOR_AGENT_SERVER_CONNECTION))
                .state(LoginCycleKeys.STATE_CHECK_LOGIN_FAILED_IMMEDIATE, b -> b
                        .guard(ctx -> {
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            return loginState != null && (loginState.getPhase() == LoginState.Phase.FAILED || loginState.getGatewayResultCode() != null);
                        })
                        .action(ctx -> {
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            org.sokybot.engine.api.login.LoginFailureVerdict verdict = w.classifyFailureVerdict(loginState, ctx);
                            String failureClass = verdict != null && verdict.getFailureClass() != null
                                    ? String.valueOf(verdict.getFailureClass().name())
                                    : w.classifyFailure(loginState, ctx);
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_LOGIN_RESPONSE_DEADLINE_MS);
                            java.util.Map<String, Object> ex = new java.util.HashMap<>();
                            ex.put("failureClass", failureClass);
                            ex.put("gatewayResultCode", verdict != null ? verdict.getGatewayCode() : null);
                            w.emitEnginePhase(ctx, LoginState.Phase.FAILED.name(), "GatewayLoginFailed",
                                    String.valueOf(loginState != null ? loginState.getFailureReason() : ""), ex);
                        })
                        .nextState(LoginCycleKeys.STATE_RETRY_DELAY)
                        .targetState(LoginCycleKeys.STATE_CHECK_LOGIN_TIMEOUT))
                .state(LoginCycleKeys.STATE_CHECK_LOGIN_TIMEOUT, b -> b
                        .guard(ctx -> {
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            if (loginState != null && loginState.getPhase() == LoginState.Phase.FAILED) {
                                return true;
                            }
                            return w.isLoginResponseTimedOut(ctx) && loginState != null && loginState.getPhase() == LoginState.Phase.LOGIN_SENT;
                        })
                        .action(ctx -> {
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            if (loginState != null && loginState.getPhase() == LoginState.Phase.FAILED) {
                                w.logWarnCtx(ctx, "Gateway login failed: {}", loginState.getFailureReason() != null ? loginState.getFailureReason() : "");
                            } else {
                                if (loginState != null) {
                                    loginState.setFailureReason("Gateway login response timeout");
                                }
                                w.logWarnCtx(ctx, "Gateway login response timed out after {} ms", w.effectiveLoginResponseTimeoutMs(settings));
                            }
                        })
                        .nextState(LoginCycleKeys.STATE_RETRY_DELAY)
                        .targetState(LoginCycleKeys.STATE_WAIT_FOR_LOGIN_RECHECK))
                .delayState(LoginCycleKeys.STATE_WAIT_FOR_LOGIN_RECHECK, b -> b.delay(500).nextState(LoginCycleKeys.STATE_WAIT_FOR_LOGIN_RESPONSE))
                .state(LoginCycleKeys.STATE_WAIT_FOR_AGENT_AUTH, b -> b
                        .guard(ctx -> {
                            LoginWorkflowSupport.FlowSnapshot s = w.snapshot(ctx);
                            return w.isAuthenticated(s.loginState) || s.loggedIn;
                        })
                        .action(ctx -> { })
                        .nextState(LoginCycleKeys.STATE_WAIT_FOR_CHARACTER)
                        .targetState(LoginCycleKeys.STATE_CHECK_AGENT_AUTH_TIMEOUT))
                .state(LoginCycleKeys.STATE_CHECK_AGENT_AUTH_TIMEOUT, b -> b
                        .guard(ctx -> {
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            if (loginState != null && loginState.getPhase() == LoginState.Phase.FAILED) {
                                return true;
                            }
                            if (!w.isAgentAuthTimedOut(ctx)) {
                                return false;
                            }
                            if (w.isLoggedIn(ctx)) {
                                return false;
                            }
                            return !w.isAuthenticated(loginState);
                        })
                        .action(ctx -> {
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            if (loginState != null && loginState.getPhase() != LoginState.Phase.FAILED) {
                                loginState.setFailureReason("Agent authentication timeout");
                            }
                            w.logWarnCtx(ctx, "Agent authentication timed out after {} ms (phase={})",
                                    w.effectiveAgentAuthTimeoutMs(settings), loginState != null ? loginState.getPhase() : null);
                        })
                        .nextState(LoginCycleKeys.STATE_RETRY_DELAY)
                        .targetState(LoginCycleKeys.STATE_WAIT_FOR_AGENT_AUTH_RECHECK))
                .delayState(LoginCycleKeys.STATE_WAIT_FOR_AGENT_AUTH_RECHECK, b -> b.delay(500).nextState(LoginCycleKeys.STATE_WAIT_FOR_AGENT_AUTH))
                .state(LoginCycleKeys.STATE_WAIT_FOR_CHARACTER, b -> b
                        .guard(ctx -> {
                            if (w.isLoggedIn(ctx)) {
                                return true;
                            }
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            if (loginState != null && loginState.getPhase() == LoginState.Phase.LOADING_ENVIRONMENT) {
                                w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_LOADING_ENVIRONMENT, "LoadingEnvironment", null);
                                return false;
                            }
                            if (w.isAuthenticated(loginState)) {
                                java.util.List<String> availableCharacters = loginState.getAvailableCharacterNames();
                                String selectedCharacter = String.valueOf(settings != null ? settings.getSelectedCharacter() : "").trim();
                                int slotBase = settings != null ? settings.getCharacterSlotBase() : 0;
                                int selectedSlotRaw = settings != null ? settings.getSelectedCharacterSlot() : -1;
                                boolean strict = settings != null && settings.isCharacterSelectionStrictMode();
                                String slotName = null;
                                if (selectedSlotRaw >= slotBase) {
                                    int idx = selectedSlotRaw - slotBase;
                                    if (idx >= 0 && idx < availableCharacters.size()) {
                                        slotName = String.valueOf(availableCharacters.get(idx));
                                    } else if (!selectedCharacter.isEmpty()) {
                                        w.logWarnCtx(ctx, "Selected character slot {} out of range, falling back to name", selectedSlotRaw);
                                    } else {
                                        loginState.setFailureReason("CHARACTER_SELECTION_UNAVAILABLE");
                                        loginState.setPhase(LoginState.Phase.FAILED);
                                        return false;
                                    }
                                }
                                if (!availableCharacters.isEmpty() && !selectedCharacter.isEmpty()) {
                                    boolean nameOk = availableCharacters.contains(selectedCharacter);
                                    boolean slotOk = (slotName == null) || selectedCharacter.equals(slotName);
                                    if (!nameOk || (strict && !slotOk)) {
                                        loginState.setFailureReason("CHARACTER_SELECTION_UNAVAILABLE");
                                        loginState.setPhase(LoginState.Phase.FAILED);
                                        return false;
                                    }
                                }
                                if (!availableCharacters.isEmpty() && w.isBlank(settings != null ? settings.getSelectedCharacter() : null)) {
                                    w.emitEnginePhase(ctx, LoginCycleKeys.PHASE_MISSING_CHARACTER_SELECTION, "MissingRequirement", null);
                                    return false;
                                }
                            }
                            return false;
                        })
                        .action(ctx -> {
                            w.resetRetry(ctx);
                            ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_IN_PROGRESS, false);
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_LOGIN_HALTED_CREDENTIAL);
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_ATTEMPT_LOGIN_SETTINGS);
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_INTERACTIVE_WAIT_UNTIL_MS);
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_RETRY_ATTEMPT_ID);
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_RETRY_UNTIL_MS);
                            ctx.getPersistentData().remove("explicitConnectRequested");
                            w.emitEnginePhase(ctx, LoginState.Phase.AUTHENTICATED.name(), "Authenticated", null);
                            w.logInfoCtx(ctx, "Login/authentication successful");
                        })
                        .nextState((String) null)
                        .targetState(LoginCycleKeys.STATE_CHECK_CHARACTER_WAIT_TIMEOUT))
                .state(LoginCycleKeys.STATE_CHECK_CHARACTER_WAIT_TIMEOUT, b -> b
                        .guard(ctx -> {
                            if (w.isLoggedIn(ctx)) {
                                return false;
                            }
                            if (!w.isAgentAuthTimedOut(ctx)) {
                                return false;
                            }
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            return w.isAuthenticated(loginState);
                        })
                        .action(ctx -> {
                            LoginState loginState = ctx.getGameModel() != null ? ctx.getGameModel().getLoginState() : null;
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            if (loginState != null) {
                                loginState.setFailureReason("Character selection or in-game timeout");
                            }
                            w.logWarnCtx(ctx, "Post-auth wait timed out after {} ms (phase={})", w.effectiveAgentAuthTimeoutMs(settings),
                                    ctx.getGameModel() != null && ctx.getGameModel().getLoginState() != null
                                            ? ctx.getGameModel().getLoginState().getPhase() : null);
                        })
                        .nextState(LoginCycleKeys.STATE_RETRY_DELAY)
                        .targetState(LoginCycleKeys.STATE_WAIT_FOR_CHARACTER_RECHECK))
                .delayState(LoginCycleKeys.STATE_WAIT_FOR_CHARACTER_RECHECK, b -> b.delay(500).nextState(LoginCycleKeys.STATE_WAIT_FOR_CHARACTER))
                .state(LoginCycleKeys.STATE_RETRY_DELAY, b -> b
                        .guard(ctx -> true)
                        .action(ctx -> {
                            ctx.getPersistentData().put(LoginCycleKeys.KEY_LOGIN_IN_PROGRESS, false);
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_ATTEMPT_LOGIN_SETTINGS);
                            LoginSettingsSnapshot settings = w.resolveRuntimeSettings(ctx, true);
                            LoginWorkflowSupport.FlowSnapshot s = w.snapshot(ctx);
                            LoginState loginState = s.loginState;
                            if (w.handleInteractivePasscodeWait(ctx, loginState, s, settings)) {
                                return;
                            }
                            if (w.handleQueueWait(ctx, loginState)) {
                                return;
                            }
                            if (w.handleMissingPrereq(ctx)) {
                                return;
                            }
                            org.sokybot.engine.api.login.LoginFailureVerdict verdict = w.classifyFailureVerdict(loginState, ctx);
                            String failureClass = verdict != null && verdict.getFailureClass() != null
                                    ? String.valueOf(verdict.getFailureClass().name())
                                    : w.classifyFailure(loginState, ctx);
                            w.logInfoCtx(ctx, "Failure classified as {}", failureClass);
                            if (w.handleTerminalFailure(ctx, loginState, verdict, settings)) {
                                return;
                            }
                            w.scheduleRetry(ctx, loginState, s, failureClass, settings);
                        })
                        .delay(0)
                        .nextState(LoginCycleKeys.STATE_WAIT_RETRY_DELAY))
                .state(LoginCycleKeys.STATE_WAIT_RETRY_DELAY, b -> b
                        .guard(ctx -> {
                            try {
                                Object retryAttemptRaw = ctx.getPersistentData().get(LoginCycleKeys.KEY_RETRY_ATTEMPT_ID);
                                long retryAttemptId = (retryAttemptRaw instanceof Number) ? ((Number) retryAttemptRaw).longValue() : -1L;
                                if (retryAttemptId > 0L && retryAttemptId != w.currentAttemptId(ctx)) {
                                    return true;
                                }
                                Object retryUntilRaw = ctx.getPersistentData().get(LoginCycleKeys.KEY_RETRY_UNTIL_MS);
                                if (!(retryUntilRaw instanceof Number)) {
                                    return true;
                                }
                                long retryUntilMs = ((Number) retryUntilRaw).longValue();
                                return retryUntilMs <= 0L || System.currentTimeMillis() >= retryUntilMs;
                            } catch (Exception e) {
                                w.logErrorThrottled(ctx, "LOGIN_WAIT_RETRY_DELAY_01", e);
                                return true;
                            }
                        })
                        .action(ctx -> {
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_RETRY_UNTIL_MS);
                            ctx.getPersistentData().remove(LoginCycleKeys.KEY_RETRY_ATTEMPT_ID);
                        })
                        .nextState((String) null)
                        .targetState(LoginCycleKeys.STATE_WAIT_RETRY_DELAY_RECHECK))
                .delayState(LoginCycleKeys.STATE_WAIT_RETRY_DELAY_RECHECK, b -> b.delay(250).nextState(LoginCycleKeys.STATE_WAIT_RETRY_DELAY))
                .build();
    }
}
