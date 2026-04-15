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
        String captchaHint = " Image verification (0x2322) only appears on some gateways after this step succeeds.";
        switch (c) {
            case 1:
            case 2:
            case 0x0B:
            case 0x0C:
                return "Gateway rejected login (code " + c + "). Check username, password, and agent selection."
                        + " An immediate 0xA102 failure means the challenge step was never offered."
                        + captchaHint;
            case 0x0D:
                return "Gateway rejected login (code 13 / 0x0D). Wrong username or password, wrong shard (agent id), or account not allowed on this gateway."
                        + " If your password uses non-Latin characters, set login charset to match the official client (often windows-1252 or a regional code page)."
                        + " Ensure gateway client build (0x6100) matches your server."
                        + " An immediate 0xA102 failure means no image challenge was offered yet."
                        + captchaHint;
            case 4:
                return "This account is already connected.";
            case 6:
                return "Disconnected or blocked by the gateway.";
            default:
                return "Gateway rejected login (code " + c + ")." + captchaHint;
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
