package org.sokybot.webview.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MachineStatusStreamHandlerTest {

    @Test
    void enrichUxHintsAddsExpectedFields() throws Exception {
        MachineStatusStreamHandler handler = new MachineStatusStreamHandler();
        Method method = MachineStatusStreamHandler.class.getDeclaredMethod("enrichUxHints", Map.class);
        method.setAccessible(true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("loginPhase", "MISSING_AGENT_SERVER");

        method.invoke(handler, payload);

        assertEquals("AGENT", payload.get("uxCategory"));
        assertEquals(Boolean.TRUE, payload.get("requiresInput"));
        assertEquals(Boolean.FALSE, payload.get("fatal"));
    }

    @Test
    void enrichUxHintsKeepsExistingFatalValue() throws Exception {
        MachineStatusStreamHandler handler = new MachineStatusStreamHandler();
        Method method = MachineStatusStreamHandler.class.getDeclaredMethod("enrichUxHints", Map.class);
        method.setAccessible(true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("loginPhase", "LOGIN_SENT");
        payload.put("fatal", Boolean.TRUE);

        method.invoke(handler, payload);

        assertTrue((Boolean) payload.get("fatal"));
        assertEquals("AUTH", payload.get("uxCategory"));
    }
}
