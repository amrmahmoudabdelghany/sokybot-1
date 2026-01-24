package org.sokybot.webview.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for RSocketRequest.
 */
@DisplayName("RSocketRequest Tests")
class RSocketRequestTest {
    
    @Test
    @DisplayName("Should create request with constructor")
    void testConstructor() {
        Map<String, Object> params = Map.of("key", "value");
        RSocketRequest request = new RSocketRequest("test.method", params, "req-1");
        
        assertEquals("test.method", request.getMethod());
        assertEquals("value", request.getParams().get("key"));
        assertEquals("req-1", request.getId());
    }
    
    @Test
    @DisplayName("Should handle null params in constructor")
    void testNullParams() {
        RSocketRequest request = new RSocketRequest("test.method", null, "req-1");
        assertNotNull(request.getParams());
        assertTrue(request.getParams().isEmpty());
    }
    
    @Test
    @DisplayName("Should extract string parameter")
    void testGetString() {
        RSocketRequest request = new RSocketRequest();
        request.setParams(Map.of("name", "test", "number", 123));
        
        assertEquals("test", request.getString("name"));
        assertEquals("123", request.getString("number")); // toString conversion
        assertNull(request.getString("missing"));
        assertEquals("default", request.getString("missing", "default"));
    }
    
    @Test
    @DisplayName("Should extract int parameter")
    void testGetInt() {
        RSocketRequest request = new RSocketRequest();
        Map<String, Object> params = new HashMap<>();
        params.put("intValue", 42);
        params.put("stringValue", "100");
        params.put("doubleValue", 3.14);
        params.put("invalid", "not a number");
        request.setParams(params);
        
        assertEquals(42, request.getInt("intValue"));
        assertEquals(100, request.getInt("stringValue"));
        assertEquals(3, request.getInt("doubleValue")); // truncated
        assertNull(request.getInt("invalid"));
        assertEquals(99, request.getInt("missing", 99));
    }
    
    @Test
    @DisplayName("Should extract boolean parameter")
    void testGetBoolean() {
        RSocketRequest request = new RSocketRequest();
        request.setParams(Map.of(
            "boolTrue", true,
            "boolFalse", false,
            "stringTrue", "true",
            "stringFalse", "false"
        ));
        
        assertTrue(request.getBoolean("boolTrue"));
        assertFalse(request.getBoolean("boolFalse"));
        assertTrue(request.getBoolean("stringTrue"));
        assertFalse(request.getBoolean("stringFalse"));
        assertNull(request.getBoolean("missing"));
        assertTrue(request.getBoolean("missing", true));
    }
    
    @Test
    @DisplayName("Should extract typed parameter")
    void testGetTyped() {
        RSocketRequest request = new RSocketRequest();
        Map<String, Object> nested = Map.of("inner", "value");
        request.setParams(Map.of("map", nested, "string", "test"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = request.get("map", Map.class);
        assertEquals("value", result.get("inner"));
        
        assertNull(request.get("string", Map.class)); // wrong type
        assertNull(request.get("missing", Map.class));
    }
    
    @Test
    @DisplayName("Should have correct toString")
    void testToString() {
        RSocketRequest request = new RSocketRequest("test.method", Map.of("key", "value"), "req-1");
        String str = request.toString();
        
        assertTrue(str.contains("test.method"));
        assertTrue(str.contains("req-1"));
    }
}
