package org.sokybot.webview.login;

import org.sokybot.gamemodel.LoginState;

/**
 * User-facing strings for Silkroad gateway (0xA102) and agent auth result codes.
 */
public final class GatewayLoginMessages {

    private GatewayLoginMessages() {
    }

    /** Maps common gateway failure bytes (see Login.groovy GATEWAY_ERR_*). */
    public static String messageForGatewayCode(Integer code) {
        if (code == null) {
            return null;
        }
        int c = code & 0xFF;
        switch (c) {
            case 1:
            case 2:
            case 0x0B:
            case 0x0C:
            case 0x0D:
                return "Invalid username or password.";
            case 4:
                return "This account is already connected.";
            case 6:
                return "Disconnected or blocked by the gateway.";
            default:
                return null;
        }
    }

    public static String messageForAgentCode(Integer code) {
        if (code == null) {
            return null;
        }
        return "Game server rejected login (code " + (code & 0xFF) + ").";
    }

    /**
     * Prefer mapped codes, then raw {@link LoginState#getFailureReason()}, for onboarding copy.
     */
    public static String deriveLoginDetailMessage(
            LoginState.Phase phase,
            Integer gatewayCode,
            Integer agentCode,
            String failureReason) {
        if (phase != LoginState.Phase.FAILED && phase != LoginState.Phase.MANUAL_VERIFICATION_REQUIRED) {
            return null;
        }
        String gw = messageForGatewayCode(gatewayCode);
        if (gw != null) {
            return gw;
        }
        String ag = messageForAgentCode(agentCode);
        if (ag != null) {
            return ag;
        }
        if (failureReason != null && !failureReason.isBlank()) {
            return failureReason.trim();
        }
        return null;
    }
}
