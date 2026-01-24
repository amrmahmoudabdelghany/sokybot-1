package org.sokybot.webview;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

/**
 * Unit tests for WebviewConfigurator
 */
@DisplayName("WebviewConfigurator Tests")
class WebviewConfiguratorTest {
    
    private WebviewConfigurator configurator;
    
    @BeforeEach
    void setUp() {
        configurator = new WebviewConfigurator();
        // No RSocket service needed - testing configurator in isolation
    }
    
    @Test
    @DisplayName("Should register declarative page")
    void testAddDeclarativePage() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "div");
        schema.put("className", "test-page");
        
        configurator.addDeclarativePage(
            "testPage",
            "Test Page",
            "icons/test.svg",
            schema
        );
        
        Map<String, Object> response = configurator.handleSchemaRequest("testPage", "");
        assertNotNull(response);
        assertTrue(response.containsKey("schema"));
    }
    
    @Test
    @DisplayName("Should register and invoke action handler")
    void testRegisterActionHandler() {
        String pageId = "testPage";
        String action = "testAction";
        
        BiFunction<String, Map<String, Object>, Map<String, Object>> handler = 
            (act, data) -> {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("action", act);
                result.put("data", data);
                return result;
            };
        
        configurator.registerActionHandler(pageId, handler);
        
        Map<String, Object> testData = Map.of("key", "value");
        Map<String, Object> response = configurator.handleAction(pageId, action, testData);
        
        assertNotNull(response);
        assertTrue((Boolean) response.get("success"));
        assertEquals(action, response.get("action"));
    }
    
    @Test
    @DisplayName("Should return error for unregistered action")
    void testUnregisteredAction() {
        Map<String, Object> response = configurator.handleAction(
            "unknownPage", 
            "unknownAction", 
            Map.of()
        );
        
        assertNotNull(response);
        assertFalse((Boolean) response.getOrDefault("success", false));
        assertTrue(response.containsKey("error"));
    }
    
    @Test
    @DisplayName("Should register and invoke stream handler")
    void testRegisterStreamHandler() {
        String pageId = "testPage";
        String streamId = "testStream";
        
        Function<Map<String, Object>, Flux<Map<String, Object>>> handler = 
            (params) -> {
                Map<String, Object> data = new HashMap<>();
                data.put("value", params.getOrDefault("key", "default"));
                return Flux.just(data);
            };
        
        configurator.registerStreamHandler(pageId, streamId, handler, "testState");
        
        Map<String, Object> params = Map.of("key", "testValue");
        Flux<Map<String, Object>> stream = configurator.handleStream(pageId, streamId, params);
        
        assertNotNull(stream);
        
        Map<String, Object> result = stream.blockFirst();
        assertNotNull(result);
        assertEquals("testValue", result.get("value"));
    }
    
    @Test
    @DisplayName("Should return empty stream for unregistered stream")
    void testUnregisteredStream() {
        Flux<Map<String, Object>> stream = configurator.handleStream(
            "unknownPage", 
            "unknownStream", 
            Map.of()
        );
        
        assertNotNull(stream);
        assertNull(stream.blockFirst(), "Should return empty stream");
    }
    
    @Test
    @DisplayName("Should register schema handler")
    void testRegisterSchemaHandler() {
        String pageId = "testPage";
        
        Function<String, Map<String, Object>> handler = 
            (request) -> {
                Map<String, Object> state = new HashMap<>();
                state.put("initialized", true);
                state.put("timestamp", System.currentTimeMillis());
                return state;
            };
        
        configurator.registerSchemaHandler(pageId, handler);
        
        Map<String, Object> response = configurator.handleSchemaRequest(pageId, "test");
        
        assertNotNull(response);
        assertTrue((Boolean) response.getOrDefault("initialized", false));
    }
    
    @Test
    @DisplayName("Should handle multiple pages independently")
    void testMultiplePages() {
        // Register page 1
        Map<String, Object> schema1 = Map.of("type", "div", "page", "1");
        configurator.addDeclarativePage("page1", "Page 1", null, schema1);
        
        // Register page 2
        Map<String, Object> schema2 = Map.of("type", "div", "page", "2");
        configurator.addDeclarativePage("page2", "Page 2", null, schema2);
        
        // Verify they are independent
        Map<String, Object> response1 = configurator.handleSchemaRequest("page1", "");
        Map<String, Object> response2 = configurator.handleSchemaRequest("page2", "");
        
        assertNotNull(response1);
        assertNotNull(response2);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> schema1Retrieved = (Map<String, Object>) response1.get("schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> schema2Retrieved = (Map<String, Object>) response2.get("schema");
        
        assertEquals("1", schema1Retrieved.get("page"));
        assertEquals("2", schema2Retrieved.get("page"));
    }
    
    @Test
    @DisplayName("Should remove page and cleanup handlers")
    void testRemovePage() {
        String pageId = "testPage";
        Map<String, Object> schema = Map.of("type", "div");
        
        configurator.addDeclarativePage(pageId, "Test", null, schema);
        configurator.registerActionHandler(pageId, (a, d) -> Map.of("success", true));
        configurator.registerStreamHandler(pageId, "stream1", (p) -> Flux.empty());
        
        // Verify page exists
        Map<String, Object> response = configurator.handleSchemaRequest(pageId, "");
        assertNotNull(response);
        
        // Remove page
        configurator.removePage(pageId);
        
        // Verify page is removed
        Map<String, Object> afterRemoval = configurator.handleSchemaRequest(pageId, "");
        assertTrue(afterRemoval.isEmpty() || !afterRemoval.containsKey("schema"));
        
        // Verify action handler is removed
        Map<String, Object> actionResponse = configurator.handleAction(pageId, "test", Map.of());
        assertFalse((Boolean) actionResponse.getOrDefault("success", false));
    }
    
    @Test
    @DisplayName("Should get extension registry")
    void testGetExtensionRegistry() {
        configurator.addDeclarativePage("page1", "Page 1", "icon1.svg", Map.of("type", "div"));
        configurator.addDeclarativePage("page2", "Page 2", "icon2.svg", Map.of("type", "Card"));
        
        Map<String, Object> registry = configurator.getExtensionRegistry();
        
        assertNotNull(registry);
        assertTrue(registry.containsKey("pages"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> pages = (Map<String, Object>) registry.get("pages");
        assertEquals(2, pages.size());
        assertTrue(pages.containsKey("page1"));
        assertTrue(pages.containsKey("page2"));
    }
    
    @Test
    @DisplayName("Should register and invoke toolbar action handler")
    void testToolbarActionHandler() {
        String actionId = "testToolbar";
        
        configurator.addToolbarAction(actionId, "Test Toolbar", "Settings", Map.of("type", "dialog"));
        configurator.registerToolbarActionHandler(actionId, (action, data) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("action", action);
            return result;
        });
        
        Map<String, Object> response = configurator.handleToolbarAction(actionId, "click", Map.of());
        
        assertTrue((Boolean) response.get("success"));
        assertEquals("click", response.get("action"));
    }
    
    @Test
    @DisplayName("Should include toolbar actions in registry")
    void testToolbarActionsInRegistry() {
        configurator.addToolbarAction("toolbar1", "Toolbar 1", "Icon1", Map.of("type", "modal"));
        configurator.addToolbarAction("toolbar2", "Toolbar 2", "Icon2", null);
        
        Map<String, Object> registry = configurator.getExtensionRegistry();
        
        assertTrue(registry.containsKey("toolbarActions"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> toolbarActions = (Map<String, Object>) registry.get("toolbarActions");
        assertEquals(2, toolbarActions.size());
        assertTrue(toolbarActions.containsKey("toolbar1"));
        assertTrue(toolbarActions.containsKey("toolbar2"));
    }
    
    @Test
    @DisplayName("Should remove toolbar action")
    void testRemoveToolbarAction() {
        String actionId = "removableToolbar";
        
        configurator.addToolbarAction(actionId, "Removable", "Icon", null);
        configurator.registerToolbarActionHandler(actionId, (a, d) -> Map.of("ok", true));
        
        // Verify exists
        Map<String, Object> registry = configurator.getExtensionRegistry();
        @SuppressWarnings("unchecked")
        Map<String, Object> actions = (Map<String, Object>) registry.get("toolbarActions");
        assertTrue(actions.containsKey(actionId));
        
        // Remove
        configurator.removeToolbarAction(actionId);
        
        // Verify removed
        registry = configurator.getExtensionRegistry();
        @SuppressWarnings("unchecked")
        Map<String, Object> actionsAfter = (Map<String, Object>) registry.get("toolbarActions");
        assertFalse(actionsAfter.containsKey(actionId));
    }
}
