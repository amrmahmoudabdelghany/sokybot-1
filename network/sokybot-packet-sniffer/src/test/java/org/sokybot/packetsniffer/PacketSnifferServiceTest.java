package org.sokybot.packetsniffer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.packetsniffer.storage.JsonPacketStorage;
import reactor.core.publisher.Flux;

/**
 * Unit tests for PacketSnifferService
 */
@DisplayName("PacketSnifferService Tests")
class PacketSnifferServiceTest {
    
    private PacketSnifferService service;
    private JsonPacketStorage storage;
    private String machineName = "test-machine";
    
    @BeforeEach
    void setUp() {
        storage = new JsonPacketStorage("./test-packet-data.json");
        service = new PacketSnifferService(storage, machineName);
    }
    
    @Test
    @DisplayName("Should handle schema request")
    void testHandleSchemaRequest() {
        Map<String, Object> response = service.handleSchemaRequest("");
        
        assertNotNull(response);
        assertTrue(response.containsKey("activeTab") || response.containsKey("packetCount"));
    }
    
    @Test
    @DisplayName("Should handle switchTab action")
    void testSwitchTab() {
        Map<String, Object> data = new HashMap<>();
        data.put("tab", "tracer");
        
        Map<String, Object> response = service.handleAction("switchTab", data);
        
        assertNotNull(response);
        assertTrue((Boolean) response.getOrDefault("success", false));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) response.get("state");
        if (state != null) {
            assertEquals("tracer", state.get("activeTab"));
        }
    }
    
    @Test
    @DisplayName("Should handle clearMonitor action")
    void testClearMonitor() {
        // Clear monitor (even if empty)
        Map<String, Object> response = service.handleAction("clearMonitor", Map.of());
        
        assertNotNull(response);
        assertTrue((Boolean) response.getOrDefault("success", false));
    }
    
    @Test
    @DisplayName("Should handle togglePause action")
    void testTogglePause() {
        Map<String, Object> response = service.handleAction("togglePause", Map.of());
        
        assertNotNull(response);
        assertTrue((Boolean) response.getOrDefault("success", false));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) response.get("state");
        if (state != null) {
            assertTrue(state.containsKey("monitorEnabled"));
        }
    }
    
    @Test
    @DisplayName("Should stream packets")
    void testStreamPackets() {
        Map<String, Object> params = new HashMap<>();
        params.put("filter", "");
        params.put("includeHistory", false);
        
        Flux<Map<String, Object>> stream = service.streamPackets(params);
        
        assertNotNull(stream);
        
        // Note: In a real scenario, we'd need to create a proper ImmutablePacket
        // For now, just verify the stream is created
        assertNotNull(stream);
    }
    
    @Test
    @DisplayName("Should stream statistics")
    void testStreamStatistics() {
        Map<String, Object> params = Map.of();
        
        Flux<Map<String, Object>> stream = service.streamStatistics(params);
        
        assertNotNull(stream);
        
        // Verify stream is created and can be subscribed to
        Map<String, Object> first = stream.blockFirst();
        assertNotNull(first);
        assertTrue(first.containsKey("packetCount"));
        assertTrue(first.containsKey("tracerCount"));
        assertTrue(first.containsKey("monitorEnabled"));
    }
    
    @Test
    @DisplayName("Should handle filterTracer action")
    void testFilterTracer() {
        Map<String, Object> data = new HashMap<>();
        data.put("value", "test");
        
        Map<String, Object> response = service.handleAction("filterTracer", data);
        
        assertNotNull(response);
        assertTrue((Boolean) response.getOrDefault("success", false));
    }
    
    @Test
    @DisplayName("Should shutdown gracefully")
    void testShutdown() {
        // Should not throw exception
        assertDoesNotThrow(() -> service.shutdown());
    }
}
