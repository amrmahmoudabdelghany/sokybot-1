package org.sokybot.login.policies.builtin;

import org.sokybot.gamemodel.LoginState;

final class LoginFailureStateInspector {

    private LoginFailureStateInspector() {
    }

    static boolean isMissingPrereqPhase(LoginState loginState) {
        if (loginState == null) {
            return false;
        }
        LoginState.Phase phase = loginState.getPhase();
        return phase == LoginState.Phase.MISSING_CREDENTIALS || phase == LoginState.Phase.MISSING_AGENT_SERVER;
    }

    static boolean isInQueueState(LoginState loginState) {
        if (loginState == null) {
            return false;
        }
        if (loginState.getPhase() == LoginState.Phase.IN_QUEUE) {
            return true;
        }
        String reason = String.valueOf(loginState.getFailureReason() == null ? "" : loginState.getFailureReason()).toLowerCase();
        return reason.contains("queue");
    }

    static boolean isServerInspectionState(LoginState loginState) {
        if (loginState == null) {
            return false;
        }
        LoginState.Phase phase = loginState.getPhase();
        if (phase == LoginState.Phase.SERVER_INSPECTION) {
            return true;
        }
        String reason = String.valueOf(loginState.getFailureReason() == null ? "" : loginState.getFailureReason()).toLowerCase();
        return reason.contains("inspection");
    }

    static boolean requiresManualVerification(LoginState loginState) {
        if (loginState == null) {
            return false;
        }
        LoginState.Phase phase = loginState.getPhase();
        if (phase == LoginState.Phase.MANUAL_VERIFICATION_REQUIRED) {
            return true;
        }
        String reason = String.valueOf(loginState.getFailureReason() == null ? "" : loginState.getFailureReason()).toLowerCase();
        return reason.contains("captcha") || reason.contains("manual");
    }

    static Integer readGatewayFailureCode(LoginState loginState) {
        if (loginState == null) {
            return null;
        }
        Integer gatewayCode = loginState.getGatewayResultCode();
        if (gatewayCode != null) {
            return gatewayCode;
        }
        return LoginFailureReasonHeuristics.parseGatewayFailureCode(loginState.getFailureReason());
    }
}
