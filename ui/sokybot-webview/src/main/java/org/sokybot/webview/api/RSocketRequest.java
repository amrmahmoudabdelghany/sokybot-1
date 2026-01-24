package org.sokybot.webview.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Structured RSocket request following JSON-RPC-like format.
 * 
 * Example JSON:
 * {
 *   "method": "character.state",
 *   "params": { "machineId": "group1.machine1" },
 *   "id": "req-123"
 * }
 */
public class RSocketRequest {
    
    private String method;
    private Map<String, Object> params;
    private String id;
    
    public RSocketRequest() {
        this.params = new HashMap<>();
    }
    
    public RSocketRequest(String method, Map<String, Object> params, String id) {
        this.method = method;
        this.params = params != null ? params : new HashMap<>();
        this.id = id;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public Map<String, Object> getParams() {
        return params;
    }
    
    public void setParams(Map<String, Object> params) {
        this.params = params != null ? params : new HashMap<>();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    // Convenience methods for extracting typed parameters
    
    public String getString(String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }
    
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }
    
    public Integer getInt(String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public int getInt(String key, int defaultValue) {
        Integer value = getInt(key);
        return value != null ? value : defaultValue;
    }
    
    public Boolean getBoolean(String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = params.get(key);
        if (value == null) return null;
        if (type.isInstance(value)) return (T) value;
        return null;
    }
    
    @Override
    public String toString() {
        return "RSocketRequest{method='" + method + "', params=" + params + ", id='" + id + "'}";
    }
}
