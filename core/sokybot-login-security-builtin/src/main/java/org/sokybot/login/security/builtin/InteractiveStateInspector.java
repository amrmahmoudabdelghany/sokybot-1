package org.sokybot.login.security.builtin;

import org.sokybot.gamemodel.LoginState;

final class InteractiveStateInspector {

    boolean isPasscodeOrCaptchaRequired(LoginState loginState) {
        if (loginState == null) {
            return false;
        }
        LoginState.Phase phase = loginState.getPhase();
        if (phase == LoginState.Phase.WAIT_FOR_CAPTCHA || phase == LoginState.Phase.WAITING_FOR_PASSCODE) {
            return true;
        }
        String reason = String.valueOf(loginState.getFailureReason() == null ? "" : loginState.getFailureReason()).toLowerCase();
        return reason.contains("passcode")
                || reason.contains("captcha")
                || reason.contains("verification code")
                || reason.contains("security code")
                || reason.contains("secondary password")
                || reason.contains("image code");
    }

    boolean isQueueState(LoginState loginState) {
        if (loginState == null) {
            return false;
        }
        if (loginState.getPhase() == LoginState.Phase.IN_QUEUE) {
            return true;
        }
        String reason = String.valueOf(loginState.getFailureReason() == null ? "" : loginState.getFailureReason()).toLowerCase();
        return reason.contains("queue");
    }

    boolean isMissingPrereqState(LoginState loginState) {
        if (loginState == null) {
            return false;
        }
        LoginState.Phase phase = loginState.getPhase();
        return phase == LoginState.Phase.MISSING_CREDENTIALS
                || phase == LoginState.Phase.MISSING_AGENT_SERVER
                || phase == LoginState.Phase.MISSING_GATEWAY
                || phase == LoginState.Phase.MISSING_CHARACTER_SELECTION
                || phase == LoginState.Phase.PENDING_MANUAL_CONNECT;
    }
}
