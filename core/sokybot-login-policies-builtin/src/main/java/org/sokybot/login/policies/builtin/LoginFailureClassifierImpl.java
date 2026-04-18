package org.sokybot.login.policies.builtin;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.login.GatewayErrorClassifier;
import org.sokybot.engine.api.login.GatewayFailureClassification;
import org.sokybot.engine.api.login.ILoginFailureClassifier;
import org.sokybot.engine.api.login.LoginFailureClass;
import org.sokybot.engine.api.login.LoginFailureVerdict;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.LoginState;

@Component(service = ILoginFailureClassifier.class, immediate = true)
public final class LoginFailureClassifierImpl implements ILoginFailureClassifier {

    private static final String KEY_AGENT_WAIT_DEADLINE_MS = "loginAgentWaitDeadlineMs";

    @Override
    public LoginFailureVerdict classify(IWorkflowContext context, String fallbackReason) {
        if (context == null || context.getGameModel() == null) {
            return verdict(LoginFailureClass.UNKNOWN_RETRY, null, fallbackReason, false, 0L, false);
        }

        LoginState loginState = context.getGameModel().getLoginState();
        if (LoginFailureStateInspector.isMissingPrereqPhase(loginState)) {
            return verdict(LoginFailureClass.MISSING_PREREQ, null, safeReason(loginState, fallbackReason), true, 0L, false);
        }
        if (LoginFailureStateInspector.isServerInspectionState(loginState)) {
            return verdict(LoginFailureClass.SERVER_INSPECTION, null, safeReason(loginState, fallbackReason), false, 60000L, false);
        }
        if (isAgentWaitTimedOut(context)) {
            return verdict(LoginFailureClass.AGENT_TIMEOUT, null, safeReason(loginState, fallbackReason), false, 0L, false);
        }
        if (LoginFailureStateInspector.requiresManualVerification(loginState)) {
            return verdict(LoginFailureClass.MANUAL_VERIFICATION, null, safeReason(loginState, fallbackReason), true, 0L, true);
        }
        if (LoginFailureStateInspector.isInQueueState(loginState)) {
            return verdict(LoginFailureClass.NETWORK, null, safeReason(loginState, fallbackReason), false, 0L, false);
        }

        String reason = safeReason(loginState, fallbackReason);
        String lower = reason.toLowerCase();
        Integer gatewayCode = LoginFailureStateInspector.readGatewayFailureCode(loginState);
        if (gatewayCode != null) {
            Optional<GatewayFailureClassification> classified =
                    GatewayErrorClassifier.classify(gatewayCode.intValue(), lower);
            if (classified.isPresent()) {
                switch (classified.get()) {
                    case GHOST_COOLDOWN:
                        return verdict(LoginFailureClass.GHOST_COOLDOWN, gatewayCode, reason, false, 60000L, false);
                    case NETWORK:
                        return verdict(LoginFailureClass.NETWORK, gatewayCode, reason, false, 0L, false);
                    case CREDENTIAL:
                        return verdict(LoginFailureClass.CREDENTIAL, gatewayCode, reason, true, 0L, true);
                    default:
                        break;
                }
            }
            return verdict(LoginFailureClass.UNKNOWN_RETRY, gatewayCode, reason, false, 0L, false);
        }

        if (LoginFailureReasonHeuristics.isCredentialReason(lower)) {
            return verdict(LoginFailureClass.CREDENTIAL, null, reason, true, 0L, true);
        }
        if (LoginFailureReasonHeuristics.isGhostCooldownReason(lower)) {
            return verdict(LoginFailureClass.GHOST_COOLDOWN, null, reason, false, 60000L, false);
        }
        if (LoginFailureReasonHeuristics.isCharacterNotFoundReason(lower)) {
            return verdict(LoginFailureClass.CHARACTER_NOT_FOUND, null, reason, true, 0L, false);
        }
        if (LoginFailureReasonHeuristics.isServerInspectionReason(lower)) {
            return verdict(LoginFailureClass.SERVER_INSPECTION, null, reason, false, 60000L, false);
        }
        if (LoginFailureReasonHeuristics.isAgentBanReason(lower)) {
            return verdict(LoginFailureClass.NETWORK, null, reason, false, 0L, false);
        }
        if (!isServerConnected(context)) {
            return verdict(LoginFailureClass.NETWORK, gatewayCode, reason, false, 0L, false);
        }
        if (LoginFailureReasonHeuristics.isNetworkReason(lower)) {
            return verdict(LoginFailureClass.NETWORK, gatewayCode, reason, false, 0L, false);
        }
        return verdict(LoginFailureClass.UNKNOWN_RETRY, gatewayCode, reason, false, 0L, false);
    }

    private static LoginFailureVerdict verdict(
            LoginFailureClass failureClass,
            Integer gatewayCode,
            String detail,
            boolean terminal,
            long retryDelayFloorMs,
            boolean haltRequired
    ) {
        return new LoginFailureVerdict(failureClass, gatewayCode, detail, terminal, retryDelayFloorMs, haltRequired);
    }

    private static String safeReason(LoginState loginState, String fallback) {
        if (loginState != null && loginState.getFailureReason() != null && !loginState.getFailureReason().isEmpty()) {
            return loginState.getFailureReason();
        }
        return fallback == null ? "" : fallback;
    }

    private static boolean isServerConnected(IWorkflowContext context) {
        try {
            return context.getDispatcher() != null && context.getDispatcher().isServerConnected();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isAgentWaitTimedOut(IWorkflowContext context) {
        try {
            Object raw = context.getPersistentData().get(KEY_AGENT_WAIT_DEADLINE_MS);
            if (!(raw instanceof Number)) {
                return false;
            }
            long deadlineMs = ((Number) raw).longValue();
            return deadlineMs > 0L && System.currentTimeMillis() >= deadlineMs;
        } catch (Exception ignored) {
            return false;
        }
    }
}
