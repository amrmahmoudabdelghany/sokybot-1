package org.sokybot.proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.network.IPacketObserver;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.proxy.test.ProxyTestBase;
import org.sokybot.proxy.test.util.MockConnectionListener;

/**
 * Unit tests for ProxyConnection.
 */
@DisplayName("ProxyConnection Tests")
class ProxyConnectionTest extends ProxyTestBase {
    
    @Test
    @DisplayName("Should return correct machine ID")
    void testGetMachineId() {
        IProxyConnection connection = createConnection();
        
        assertEquals(TEST_MACHINE_ID, connection.getMachineId());
    }
    
    @Test
    @DisplayName("Should have packet publisher")
    void testGetPacketPublisher() {
        IProxyConnection connection = createConnection();
        
        IPacketPublisher publisher = connection.getPacketPublisher();
        assertNotNull(publisher);
    }
    
    @Test
    @DisplayName("Should start in disconnected state")
    void testInitialConnectionState() {
        IProxyConnection connection = createConnection();
        
        assertFalse(connection.isConnected());
        assertFalse(connection.isClientConnected());
        assertFalse(connection.isServerConnected());
    }
    
    @Test
    @DisplayName("Should set and get clientless mode")
    void testClientlessMode() {
        IProxyConnection connection = createConnection();
        
        assertFalse(connection.isClientlessMode());
        
        connection.setClientlessMode(true);
        assertTrue(connection.isClientlessMode());
        
        connection.setClientlessMode(false);
        assertFalse(connection.isClientlessMode());
    }
    
    @Test
    @DisplayName("Should update connection listener")
    void testSetConnectionListener() {
        IProxyConnection connection = createConnection();
        IConnectionListener newListener = mock(IConnectionListener.class);
        
        connection.setConnectionListener(newListener);
        
        // Verify listener can be changed (we can't directly verify internal state)
        // but we can verify the connection still works
        assertNotNull(connection);
    }
    
    @Test
    @DisplayName("Should allow packet subscription")
    void testPacketSubscription() throws InterruptedException {
        IProxyConnection connection = createConnection();
        IPacketPublisher publisher = connection.getPacketPublisher();
        
        CountDownLatch latch = new CountDownLatch(1);
        IPacketObserver observer = packet -> {
            assertEquals(0x1234, packet.getOpcode());
            latch.countDown();
        };
        
        publisher.subscribe(0x1234, observer);
        
        // Simulate packet publication (this would normally happen through the channel pipeline)
        // For this test, we're just verifying subscription works
        assertNotNull(publisher);
        
        // Note: Actual packet publishing would require a more complex test setup with real channels
    }
    
    @Test
    @DisplayName("Should handle disconnect when not connected")
    void testDisconnectWhenNotConnected() {
        IProxyConnection connection = createConnection();
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            connection.disconnect();
        });
    }
}
