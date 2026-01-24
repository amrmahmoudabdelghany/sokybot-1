package org.sokybot.webview;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

/**
 * Integration tests for RSocket communication between backend and frontend.
 * Tests the WebviewConfigurator independently of RSocketServerService.
 */
@DisplayName("RSocket Integration Tests")
class RSocketIntegrationTest {
    
    private WebviewConfigurator configurator;
    
    @BeforeEach
    void setUp() {
        configurator = new WebviewConfigurator();
        // No RSocket service needed for these tests - testing configurator in isolation
    }
    
    @Test
    @DisplayName("Should handle schema request via RSocket")
    void testSchemaRequest() {
        String pageId = "testPage";
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "div");
        schema.put("className", "test");
        
        configurator.addDeclarativePage(pageId, "Test Page", null, schema);
        
        // Simulate RSocket request
        Map<String, Object> response = configurator.handleSchemaRequest(pageId, "");
        
        assertNotNull(response);
        assertTrue(response.containsKey("schema"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> returnedSchema = (Map<String, Object>) response.get("schema");
        assertEquals("div", returnedSchema.get("type"));
    }
    
    @Test
    @DisplayName("Should handle action request via RSocket")
    void testActionRequest() {
        String pageId = "testPage";
        
        configurator.registerActionHandler(pageId, (action, data) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("action", action);
            result.put("receivedData", data);
            return result;
        });
        
        Map<String, Object> testData = Map.of("key", "value");
        Map<String, Object> response = configurator.handleAction(pageId, "testAction", testData);
        
        assertNotNull(response);
        assertTrue((Boolean) response.get("success"));
        assertEquals("testAction", response.get("action"));
    }
    
    @Test
    @DisplayName("Should handle stream request via RSocket")
    void testStreamRequest() {
        String pageId = "testPage";
        String streamId = "testStream";
        
        configurator.registerStreamHandler(pageId, streamId, (params) -> {
            Map<String, Object> data1 = new HashMap<>();
            data1.put("value", 1);
            Map<String, Object> data2 = new HashMap<>();
            data2.put("value", 2);
            return Flux.just(data1, data2);
        });
        
        Flux<Map<String, Object>> stream = configurator.handleStream(pageId, streamId, Map.of());
        
        assertNotNull(stream);
        
        Map<String, Object> first = stream.blockFirst();
        assertNotNull(first);
        assertEquals(1, first.get("value"));
        
        Map<String, Object> second = stream.skip(1).blockFirst();
        assertNotNull(second);
        assertEquals(2, second.get("value"));
    }
    
    @Test
    @DisplayName("Should send events without throwing")
    void testSendEvent() {
        String eventType = "test.event";
        Map<String, Object> eventData = Map.of("message", "test");
        
        // Should not throw exception even without RSocket service
        assertDoesNotThrow(() -> {
            configurator.sendEvent(eventType, eventData);
        });
    }
    
    @Test
    @DisplayName("Should handle multiple concurrent requests")
    void testConcurrentRequests() throws InterruptedException {
        String pageId = "concurrentPage";
        configurator.addDeclarativePage(pageId, "Concurrent", null, Map.of("type", "div"));
        
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    Map<String, Object> response = configurator.handleSchemaRequest(pageId, "");
                    assertNotNull(response);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete");
    }
    
    @Test
    @DisplayName("Should return error for missing action handler")
    void testMissingActionHandler() {
        Map<String, Object> response = configurator.handleAction("nonexistent", "action", Map.of());
        
        assertFalse((Boolean) response.get("success"));
        assertTrue(response.containsKey("error"));
    }
    
    @Test
    @DisplayName("Should return empty flux for missing stream handler")
    void testMissingStreamHandler() {
        Flux<Map<String, Object>> stream = configurator.handleStream("nonexistent", "stream", Map.of());
        
        assertNotNull(stream);
        assertEquals(0, stream.count().block());
    }
    
    @Test
    @DisplayName("Should get extension registry")
    void testExtensionRegistry() {
        configurator.addDeclarativePage("page1", "Page 1", "icon1", Map.of("type", "div"));
        configurator.addToolbarAction("action1", "Action 1", "Icon", Map.of("type", "modal"));
        
        Map<String, Object> registry = configurator.getExtensionRegistry();
        
        assertNotNull(registry);
        assertTrue(registry.containsKey("pages"));
        assertTrue(registry.containsKey("toolbarActions"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> pages = (Map<String, Object>) registry.get("pages");
        assertTrue(pages.containsKey("page1"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> toolbarActions = (Map<String, Object>) registry.get("toolbarActions");
        assertTrue(toolbarActions.containsKey("action1"));
    }
    
    @Test
    @DisplayName("Should remove page and clean up handlers")
    void testRemovePage() {
        String pageId = "removablePage";
        configurator.addDeclarativePage(pageId, "Removable", null, Map.of("type", "div"));
        configurator.registerActionHandler(pageId, (action, data) -> Map.of("ok", true));
        configurator.registerStreamHandler(pageId, "stream1", (params) -> Flux.just(Map.of("v", 1)));
        
        // Verify page exists
        Map<String, Object> registry = configurator.getExtensionRegistry();
        @SuppressWarnings("unchecked")
        Map<String, Object> pages = (Map<String, Object>) registry.get("pages");
        assertTrue(pages.containsKey(pageId));
        
        // Remove page
        configurator.removePage(pageId);
        
        // Verify page is removed
        registry = configurator.getExtensionRegistry();
        @SuppressWarnings("unchecked")
        Map<String, Object> pagesAfter = (Map<String, Object>) registry.get("pages");
        assertFalse(pagesAfter.containsKey(pageId));
        
        // Verify action handler is gone
        Map<String, Object> actionResponse = configurator.handleAction(pageId, "test", Map.of());
        assertFalse((Boolean) actionResponse.get("success"));
    }
    
    @Test
    @DisplayName("Should handle toolbar action")
    void testToolbarAction() {
        String actionId = "myToolbar";
        configurator.addToolbarAction(actionId, "My Toolbar", "Settings", Map.of("type", "dialog"));
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
}
