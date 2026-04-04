package org.sokybot.webview.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sokybot.webview.status.MachineStatusPayloadHelper;

class MachineStatusStreamHandlerTest {

    @Test
    void enrichUxHintsAddsExpectedFields() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("loginPhase", "MISSING_AGENT_SERVER");

        MachineStatusPayloadHelper.enrichUxHints(payload);

        assertEquals("AGENT", payload.get("uxCategory"));
        assertEquals(Boolean.TRUE, payload.get("requiresInput"));
        assertEquals(Boolean.FALSE, payload.get("fatal"));
    }

    @Test
    void enrichUxHintsPendingManualConnectIsInfoWithoutRequiresInput() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("loginPhase", "PENDING_MANUAL_CONNECT");

        MachineStatusPayloadHelper.enrichUxHints(payload);

        assertEquals("CONNECT", payload.get("uxCategory"));
        assertEquals(Boolean.FALSE, payload.get("requiresInput"));
        assertEquals(Boolean.FALSE, payload.get("fatal"));
    }

    @Test
    void enrichUxHintsKeepsExistingFatalValue() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("loginPhase", "LOGIN_SENT");
        payload.put("fatal", Boolean.TRUE);

        MachineStatusPayloadHelper.enrichUxHints(payload);

        assertTrue((Boolean) payload.get("fatal"));
        assertEquals("AUTH", payload.get("uxCategory"));
    }
}
