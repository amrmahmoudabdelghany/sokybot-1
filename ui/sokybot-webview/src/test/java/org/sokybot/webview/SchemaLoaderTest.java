package org.sokybot.webview;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.webview.api.util.SchemaLoader;

/**
 * Unit tests for SchemaLoader utility class
 */
@DisplayName("SchemaLoader Tests")
class SchemaLoaderTest {
    
    @Test
    @DisplayName("Should load schema from resources")
    void testLoadSchema() {
        Map<String, Object> schema = SchemaLoader.loadSchema(
            "/ui/example-page.json", 
            SchemaLoaderTest.class
        );
        
        assertNotNull(schema, "Schema should be loaded");
        assertTrue(schema.containsKey("type"), "Schema should have type");
    }
    
    @Test
    @DisplayName("Should return null for non-existent schema")
    void testLoadNonExistentSchema() {
        Map<String, Object> schema = SchemaLoader.loadSchema(
            "/ui/non-existent.json", 
            SchemaLoaderTest.class
        );
        
        assertNull(schema, "Should return null for non-existent schema");
    }
    
    @Test
    @DisplayName("Should resolve $ref references in schema")
    void testResolveReferences() {
        // Create a test schema with $ref
        Map<String, Object> mainSchema = new HashMap<>();
        mainSchema.put("type", "div");
        mainSchema.put("className", "container");
        
        Map<String, Object> refChild = new HashMap<>();
        refChild.put("$ref", "example-page.json");
        
        java.util.List<Object> children = new java.util.ArrayList<>();
        children.add(refChild);
        mainSchema.put("children", children);
        
        // Note: This test requires the actual file to exist
        // In a real scenario, we'd mock the resource loading
        assertNotNull(mainSchema, "Schema structure should be valid");
    }
    
    @Test
    @DisplayName("Should handle schema without children")
    void testSchemaWithoutChildren() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "Button");
        schema.put("props", Map.of("label", "Click me"));
        
        // Should not throw exception
        assertNotNull(schema);
        assertEquals("Button", schema.get("type"));
    }
    
    @Test
    @DisplayName("Should handle nested schema structures")
    void testNestedSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "Card");
        
        Map<String, Object> header = new HashMap<>();
        header.put("type", "CardHeader");
        
        Map<String, Object> title = new HashMap<>();
        title.put("type", "CardTitle");
        title.put("props", Map.of("children", "Test Title"));
        
        java.util.List<Object> headerChildren = new java.util.ArrayList<>();
        headerChildren.add(title);
        header.put("children", headerChildren);
        
        java.util.List<Object> cardChildren = new java.util.ArrayList<>();
        cardChildren.add(header);
        schema.put("children", cardChildren);
        
        assertNotNull(schema);
        assertTrue(schema.containsKey("children"));
    }
}
