package org.sokybot.webview.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.sokybot.http.server.events.BridgeEvent;

/**
 * Builds machine status stream payloads from bridge events and adds UX hint fields.
 */
public final class MachineStatusPayloadHelper {

    private MachineStatusPayloadHelper() {
    }

    public static Map<String, Object> fromBridgeEvent(BridgeEvent event) {
        if (event == null) {
            return Map.of();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("topic", event.getTopic());
        payload.put("timestamp", System.currentTimeMillis());
        if (event.getMachineId() != null) {
            payload.put("machineId", event.getMachineId());
        }
        Object eventPayload = event.getPayload();
        if (eventPayload instanceof Map<?, ?>) {
            Map<?, ?> m = (Map<?, ?>) eventPayload;
            m.forEach((k, v) -> payload.put(String.valueOf(k), v));
        } else if (eventPayload != null) {
            payload.put("payload", eventPayload);
        }
        enrichUxHints(payload);
        return payload;
    }

    public static void enrichUxHints(Map<String, Object> payload) {
        String loginPhase = asString(payload.get("loginPhase"));
        if (loginPhase == null || loginPhase.isEmpty()) {
            return;
        }
        if (!payload.containsKey("uxCategory")) {
            payload.put("uxCategory", uxCategoryFor(loginPhase));
        }
        if (!payload.containsKey("requiresInput")) {
            payload.put("requiresInput", requiresInputFor(loginPhase));
        }
        if (!payload.containsKey("fatal")) {
            payload.put("fatal", fatalFor(loginPhase));
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String uxCategoryFor(String phase) {
        switch (phase) {
            case "DISCONNECTED":
            case "MISSING_GATEWAY":
            case "PENDING_MANUAL_CONNECT":
            case "CONNECTING_GATEWAY":
                return "CONNECT";
            case "WAITING_FOR_AGENTS":
            case "WAITING_FOR_AGENTS_TIMEOUT":
            case "MISSING_AGENT_SERVER":
            case "SERVER_INSPECTION":
                return "AGENT";
            case "MISSING_CHARACTER_SELECTION":
                return "CHARACTER";
            case "IN_GAME":
            case "LOADING_ENVIRONMENT":
                return "INGAME";
            case "FAILED":
            case "MANUAL_VERIFICATION_REQUIRED":
            case "RETRY_DISABLED":
            case "RETRY_LIMIT_REACHED":
                return "ERROR";
            default:
                return "AUTH";
        }
    }

    private static boolean requiresInputFor(String phase) {
        return "MISSING_GATEWAY".equals(phase)
                || "MISSING_AGENT_SERVER".equals(phase)
                || "MISSING_CREDENTIALS".equals(phase)
                || "MISSING_CHARACTER_SELECTION".equals(phase)
                || "WAITING_FOR_PASSCODE".equals(phase)
                || "WAIT_FOR_CAPTCHA".equals(phase)
                || "MANUAL_VERIFICATION_REQUIRED".equals(phase);
    }

    private static boolean fatalFor(String phase) {
        return "FAILED".equals(phase)
                || "MANUAL_VERIFICATION_REQUIRED".equals(phase)
                || "RETRY_DISABLED".equals(phase)
                || "RETRY_LIMIT_REACHED".equals(phase);
    }
}
