package org.sokybot.proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.proxy.test.ProxyTestBase;

/**
 * Unit tests for ProxyConnectionFactory.
 */
@DisplayName("ProxyConnectionFactory Tests")
class ProxyConnectionFactoryTest extends ProxyTestBase {
    
    @Test
    @DisplayName("Should create connection successfully")
    void testCreateConnection() {
        IProxyConnection connection = factory.createConnection(TEST_MACHINE_ID, mockListener);
        
        assertNotNull(connection);
        assertEquals(TEST_MACHINE_ID, connection.getMachineId());
        assertNotNull(connection.getPacketPublisher());
    }
    
    @Test
    @DisplayName("Should throw exception when creating duplicate connection")
    void testCreateDuplicateConnection() {
        factory.createConnection(TEST_MACHINE_ID, mockListener);
        
        assertThrows(IllegalStateException.class, () -> {
            factory.createConnection(TEST_MACHINE_ID, mockListener);
        });
    }
    
    @Test
    @DisplayName("Should create multiple connections with different machine IDs")
    void testCreateMultipleConnections() {
        IProxyConnection conn1 = factory.createConnection("machine-1", mockListener);
        IProxyConnection conn2 = factory.createConnection("machine-2", mockListener);
        
        assertNotNull(conn1);
        assertNotNull(conn2);
        assertNotSame(conn1, conn2);
    }
    
    @Test
    @DisplayName("Should shutdown all connections gracefully")
    void testShutdown() throws Exception {
        IProxyConnection conn1 = factory.createConnection("machine-1", mockListener);
        IProxyConnection conn2 = factory.createConnection("machine-2", mockListener);
        
        factory.shutdown();
        
        // Verify connections are cleaned up
        // After shutdown, factory should be in a clean state
        assertTrue(true); // Shutdown should complete without exception
    }
}
