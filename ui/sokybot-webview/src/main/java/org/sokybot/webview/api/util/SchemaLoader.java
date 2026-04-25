package org.sokybot.webview.api.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for loading and resolving JSON UI schemas.
 * 
 * This utility is provided as part of the webview extension API to help bundles
 * load their declarative UI schemas from resources and resolve $ref references.
 * 
 * Usage example:
 * <pre>
 * Map&lt;String, Object&gt; schema = SchemaLoader.loadSchema("/ui/my-page.json", MyClass.class);
 * </pre>
 */
public class SchemaLoader {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Load a schema from resources and resolve $ref references.
     * 
     * @param resourcePath Path to the JSON schema file in resources (e.g., "/ui/page.json")
     * @param clazz Class to use for resource loading (typically the bundle's activator class)
     * @return The loaded and resolved schema as a Map, or null if loading failed
     */
    public static Map<String, Object> loadSchema(String resourcePath, Class<?> clazz) {
        try (InputStream stream = clazz.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                Map<String, Object> schema = mapper.readValue(stream, Map.class);
                
                // Resolve $ref references
                resolveReferences(schema, clazz);
                
                return schema;
            }
        } catch (java.io.IOException | RuntimeException e) {
            System.err.println("Failed to load schema from " + resourcePath + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private static void resolveReferences(Map<String, Object> schema, Class<?> clazz) {
        if (schema == null) return;
        
        // Resolve $ref in children arrays
        if (schema.containsKey("children")) {
            Object children = schema.get("children");
            if (children instanceof List) {
                List<Object> childrenList = (List<Object>) children;
                for (int i = 0; i < childrenList.size(); i++) {
                    Object child = childrenList.get(i);
                    if (child instanceof Map) {
                        Map<String, Object> childMap = (Map<String, Object>) child;
                        if (childMap.containsKey("$ref")) {
                            String ref = (String) childMap.get("$ref");
                            Map<String, Object> resolved = loadSchema("/ui/" + ref, clazz);
                            if (resolved != null) {
                                // Merge resolved schema with any additional props from the ref
                                Map<String, Object> merged = new HashMap<>(resolved);
                                // Preserve any props that were in the ref object
                                for (Map.Entry<String, Object> entry : childMap.entrySet()) {
                                    if (!entry.getKey().equals("$ref")) {
                                        merged.put(entry.getKey(), entry.getValue());
                                    }
                                }
                                childrenList.set(i, merged);
                            } else {
                                // Remove the ref if it couldn't be resolved
                                childrenList.remove(i);
                                i--;
                            }
                        } else {
                            resolveReferences(childMap, clazz);
                        }
                    }
                }
            }
        }
        
        // Recursively resolve in all nested objects
        for (Object value : schema.values()) {
            if (value instanceof Map) {
                Map<String, Object> nested = (Map<String, Object>) value;
                resolveReferences(nested, clazz);
            } else if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                for (Object item : list) {
                    if (item instanceof Map) {
                        resolveReferences((Map<String, Object>) item, clazz);
                    }
                }
            }
        }
    }
}
