package org.sokybot.webview.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for RSocketResponse.
 */
@DisplayName("RSocketResponse Tests")
class RSocketResponseTest {
    
    @Test
    @DisplayName("Should create success response")
    void testSuccessResponse() {
        RSocketResponse response = RSocketResponse.success(Map.of("key", "value"));
        
        assertTrue(response.isSuccess());
        assertFalse(response.isError());
        assertNotNull(response.getResult());
        assertNull(response.getError());
    }
    
    @Test
    @DisplayName("Should create success response with ID")
    void testSuccessWithId() {
        RSocketResponse response = RSocketResponse.success("result", "req-1");
        
        assertEquals("result", response.getResult());
        assertEquals("req-1", response.getId());
    }
    
    @Test
    @DisplayName("Should create error response")
    void testErrorResponse() {
        RSocketResponse response = RSocketResponse.error(404, "Not found");
        
        assertFalse(response.isSuccess());
        assertTrue(response.isError());
        assertNull(response.getResult());
        assertEquals(404, response.getError().getCode());
        assertEquals("Not found", response.getError().getMessage());
    }
    
    @Test
    @DisplayName("Should create error response with ID")
    void testErrorWithId() {
        RSocketResponse response = RSocketResponse.error(500, "Error", "req-1");
        
        assertEquals("req-1", response.getId());
        assertEquals(500, response.getError().getCode());
    }
    
    @Test
    @DisplayName("Should create not found response")
    void testNotFound() {
        RSocketResponse response = RSocketResponse.notFound("Resource not found");
        
        assertTrue(response.isError());
        assertEquals(404, response.getError().getCode());
    }
    
    @Test
    @DisplayName("Should create invalid params response")
    void testInvalidParams() {
        RSocketResponse response = RSocketResponse.invalidParams("Missing parameter");
        
        assertTrue(response.isError());
        assertEquals(RSocketResponse.ErrorCode.INVALID_PARAMS, response.getError().getCode());
    }
    
    @Test
    @DisplayName("Should create method not found response")
    void testMethodNotFound() {
        RSocketResponse response = RSocketResponse.methodNotFound("unknown.method");
        
        assertTrue(response.isError());
        assertEquals(RSocketResponse.ErrorCode.METHOD_NOT_FOUND, response.getError().getCode());
        assertTrue(response.getError().getMessage().contains("unknown.method"));
    }
    
    @Test
    @DisplayName("Should create internal error response")
    void testInternalError() {
        RSocketResponse response = RSocketResponse.internalError("Something went wrong");
        
        assertTrue(response.isError());
        assertEquals(RSocketResponse.ErrorCode.INTERNAL_ERROR, response.getError().getCode());
    }
    
    @Test
    @DisplayName("Should create internal error from exception")
    void testInternalErrorFromException() {
        Exception e = new RuntimeException("Test exception");
        RSocketResponse response = RSocketResponse.internalError(e);
        
        assertTrue(response.isError());
        assertEquals(RSocketResponse.ErrorCode.INTERNAL_ERROR, response.getError().getCode());
        assertTrue(response.getError().getMessage().contains("Test exception"));
    }
    
    @Test
    @DisplayName("Should handle exception with null message")
    void testInternalErrorNullMessage() {
        Exception e = new RuntimeException((String) null);
        RSocketResponse response = RSocketResponse.internalError(e);
        
        assertEquals("RuntimeException", response.getError().getMessage());
    }
    
    @Test
    @DisplayName("Should convert success to map")
    void testSuccessToMap() {
        RSocketResponse response = RSocketResponse.success(Map.of("key", "value"), "req-1");
        Map<String, Object> map = response.toMap();
        
        assertNotNull(map.get("result"));
        assertNull(map.get("error"));
        assertEquals("req-1", map.get("id"));
    }
    
    @Test
    @DisplayName("Should convert error to map")
    void testErrorToMap() {
        RSocketResponse response = RSocketResponse.error(500, "Error", "req-1");
        Map<String, Object> map = response.toMap();
        
        assertNull(map.get("result"));
        assertNotNull(map.get("error"));
        assertEquals("req-1", map.get("id"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorMap = (Map<String, Object>) map.get("error");
        assertEquals(500, errorMap.get("code"));
        assertEquals("Error", errorMap.get("message"));
    }
    
    @Test
    @DisplayName("Should include error data in map")
    void testErrorDataInMap() {
        RSocketResponse response = RSocketResponse.error(400, "Bad request");
        response.getError().setData(Map.of("field", "name"));
        
        Map<String, Object> map = response.toMap();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorMap = (Map<String, Object>) map.get("error");
        assertNotNull(errorMap.get("data"));
    }
    
    @Test
    @DisplayName("Should have correct error codes")
    void testErrorCodes() {
        assertEquals(-32700, RSocketResponse.ErrorCode.PARSE_ERROR);
        assertEquals(-32600, RSocketResponse.ErrorCode.INVALID_REQUEST);
        assertEquals(-32601, RSocketResponse.ErrorCode.METHOD_NOT_FOUND);
        assertEquals(-32602, RSocketResponse.ErrorCode.INVALID_PARAMS);
        assertEquals(-32603, RSocketResponse.ErrorCode.INTERNAL_ERROR);
        assertEquals(404, RSocketResponse.ErrorCode.NOT_FOUND);
        assertEquals(401, RSocketResponse.ErrorCode.UNAUTHORIZED);
        assertEquals(403, RSocketResponse.ErrorCode.FORBIDDEN);
        assertEquals(409, RSocketResponse.ErrorCode.CONFLICT);
        assertEquals(503, RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE);
    }
}
