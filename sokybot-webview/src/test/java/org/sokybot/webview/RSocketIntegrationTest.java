package org.sokybot.webview;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

/**
 * Integration tests for RSocket communication between backend and frontend
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RSocket Integration Tests")
class RSocketIntegrationTest {
    
    private WebviewConfigurator configurator;
    private RSocketServerService rsocketService;
    
    @Mock
    private GameEventBridge eventBridge;
    
    @BeforeEach
    void setUp() {
        rsocketService = new RSocketServerService();
        rsocketService.setEventBridge(eventBridge);
        
        configurator = new WebviewConfigurator();
        configurator.setRSocketService(rsocketService);
        rsocketService.registerExtensionConfigurator(configurator);
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
    void testStreamRequest() throws InterruptedException {
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
    @DisplayName("Should send events to frontend")
    void testSendEvent() {
        String eventType = "test.event";
        Map<String, Object> eventData = Map.of("message", "test");
        
        // Should not throw exception
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
            final int index = i;
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
}
