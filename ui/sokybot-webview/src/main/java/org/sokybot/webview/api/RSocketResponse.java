package org.sokybot.webview.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Structured RSocket response following JSON-RPC-like format.
 * 
 * Success response:
 * {
 * "result": { ... },
 * "id": "req-123"
 * }
 * 
 * Error response:
 * {
 * "error": { "code": 404, "message": "Not found" },
 * "id": "req-123"
 * }
 */
public class RSocketResponse {

    private Object result;
    private RSocketError error;
    private String id;

    private RSocketResponse() {
    }

    /**
     * Create a successful response with result data.
     */
    public static RSocketResponse success(Object result) {
        RSocketResponse response = new RSocketResponse();
        response.result = result;
        return response;
    }

    /**
     * Create a successful response with result data and request ID.
     */
    public static RSocketResponse success(Object result, String id) {
        RSocketResponse response = success(result);
        response.id = id;
        return response;
    }

    /**
     * Create an error response.
     */
    public static RSocketResponse error(int code, String message) {
        RSocketResponse response = new RSocketResponse();
        response.error = new RSocketError(code, message);
        return response;
    }

    /**
     * Create an error response with request ID.
     */
    public static RSocketResponse error(int code, String message, String id) {
        RSocketResponse response = error(code, message);
        response.id = id;
        return response;
    }

    /**
     * Create a not found error response.
     */
    public static RSocketResponse notFound(String message) {
        return error(ErrorCode.NOT_FOUND, message);
    }

    /**
     * Create a not found error response with request ID.
     */
    public static RSocketResponse notFound(String message, String id) {
        return error(ErrorCode.NOT_FOUND, message, id);
    }

    /**
     * Create an invalid params error response.
     */
    public static RSocketResponse invalidParams(String message) {
        return error(ErrorCode.INVALID_PARAMS, message);
    }

    /**
     * Create a method not found error response.
     */
    public static RSocketResponse methodNotFound(String method) {
        return error(ErrorCode.METHOD_NOT_FOUND, "Method not found: " + method);
    }

    /**
     * Create an internal error response.
     */
    public static RSocketResponse internalError(String message) {
        return error(ErrorCode.INTERNAL_ERROR, message);
    }

    /**
     * Create an internal error response from exception.
     */
    public static RSocketResponse internalError(Throwable t) {
        String message = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        return error(ErrorCode.INTERNAL_ERROR, message);
    }

    // Getters and setters

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public RSocketError getError() {
        return error;
    }

    public void setError(RSocketError error) {
        this.error = error;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isError() {
        return error != null;
    }

    /**
     * Convert to Map for JSON serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (result != null) {
            map.put("result", result);
        }
        if (error != null) {
            map.put("error", error.toMap());
        }
        if (id != null) {
            map.put("id", id);
        }
        return map;
    }

    /**
     * Standard error codes (following JSON-RPC conventions).
     */
    public static class ErrorCode {
        public static final int PARSE_ERROR = -32700;
        public static final int INVALID_REQUEST = -32600;
        public static final int METHOD_NOT_FOUND = -32601;
        public static final int INVALID_PARAMS = -32602;
        public static final int INTERNAL_ERROR = -32603;

        // Application-specific error codes
        public static final int NOT_FOUND = 404;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int CONFLICT = 409;
        public static final int TIMEOUT = 408;
        public static final int SERVICE_UNAVAILABLE = 503;

        private ErrorCode() {
        }
    }

    /**
     * Error details.
     */
    public static class RSocketError {
        private int code;
        private String message;
        private Object data;

        public RSocketError() {
        }

        public RSocketError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public RSocketError(int code, String message, Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("code", code);
            map.put("message", message);
            if (data != null) {
                map.put("data", data);
            }
            return map;
        }
    }
}
