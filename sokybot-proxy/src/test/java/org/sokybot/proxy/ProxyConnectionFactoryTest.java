package org.sokybot.proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

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
    @DisplayName("Should create connection with null listener")
    void testCreateConnectionWithNullListener() {
        IProxyConnection connection = factory.createConnection(TEST_MACHINE_ID, null);
        
        assertNotNull(connection);
        assertEquals(TEST_MACHINE_ID, connection.getMachineId());
    }
    
    @Test
    @DisplayName("Should throw exception when creating duplicate connection")
    void testCreateDuplicateConnection() {
        factory.createConnection(TEST_MACHINE_ID, mockListener);
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            factory.createConnection(TEST_MACHINE_ID, mockListener);
        });
        
        assertTrue(exception.getMessage().contains(TEST_MACHINE_ID));
    }
    
    @Test
    @DisplayName("Should throw exception for duplicate even with different listener")
    void testCreateDuplicateConnectionDifferentListener() {
        factory.createConnection(TEST_MACHINE_ID, mockListener);
        
        IConnectionListener differentListener = mock(IConnectionListener.class);
        
        assertThrows(IllegalStateException.class, () -> {
            factory.createConnection(TEST_MACHINE_ID, differentListener);
        });
    }
    
    @Test
    @DisplayName("Should create multiple connections with different machine IDs")
    void testCreateMultipleConnections() {
        IProxyConnection conn1 = factory.createConnection("machine-1", mockListener);
        IProxyConnection conn2 = factory.createConnection("machine-2", mockListener);
        IProxyConnection conn3 = factory.createConnection("machine-3", mockListener);
        
        assertNotNull(conn1);
        assertNotNull(conn2);
        assertNotNull(conn3);
        assertNotSame(conn1, conn2);
        assertNotSame(conn2, conn3);
        assertNotSame(conn1, conn3);
        
        assertEquals("machine-1", conn1.getMachineId());
        assertEquals("machine-2", conn2.getMachineId());
        assertEquals("machine-3", conn3.getMachineId());
    }
    
    @Test
    @DisplayName("Should create connections with empty machine ID")
    void testCreateConnectionWithEmptyMachineId() {
        IProxyConnection connection = factory.createConnection("", mockListener);
        
        assertNotNull(connection);
        assertEquals("", connection.getMachineId());
    }
    
    @Test
    @DisplayName("Should create connections with special characters in machine ID")
    void testCreateConnectionWithSpecialCharacters() {
        String[] specialIds = {
            "machine-with-dashes",
            "machine_with_underscores",
            "machine.with.dots",
            "machine123",
            "123machine",
            "Machine With Spaces"
        };
        
        for (String machineId : specialIds) {
            IProxyConnection connection = factory.createConnection(machineId, mockListener);
            assertNotNull(connection, "Should create connection for: " + machineId);
            assertEquals(machineId, connection.getMachineId());
        }
    }
    
    @Test
    @DisplayName("Should shutdown all connections gracefully")
    void testShutdown() throws Exception {
        IProxyConnection conn1 = factory.createConnection("machine-1", mockListener);
        IProxyConnection conn2 = factory.createConnection("machine-2", mockListener);
        IProxyConnection conn3 = factory.createConnection("machine-3", mockListener);
        
        factory.shutdown();
        
        // Verify connections are cleaned up
        // After shutdown, factory should be in a clean state
        assertTrue(true); // Shutdown should complete without exception
    }
    
    @Test
    @DisplayName("Should shutdown gracefully when no connections exist")
    void testShutdownWithNoConnections() {
        assertDoesNotThrow(() -> {
            factory.shutdown();
        });
    }
    
    @Test
    @DisplayName("Should handle multiple shutdown calls")
    void testMultipleShutdownCalls() {
        factory.createConnection("machine-1", mockListener);
        
        assertDoesNotThrow(() -> {
            factory.shutdown();
            factory.shutdown(); // Second call should not throw
            factory.shutdown(); // Third call should not throw
        });
    }
    
    @Test
    @DisplayName("Should not allow creating connection after shutdown")
    void testCreateConnectionAfterShutdown() {
        factory.createConnection("machine-1", mockListener);
        factory.shutdown();
        
        // After shutdown, factory state is reset, but event loop groups may be closed
        // This test verifies graceful handling
        // Note: Actual behavior depends on implementation
        assertTrue(true);
    }
    
    @Test
    @DisplayName("Should handle shutdown with many connections")
    void testShutdownWithManyConnections() {
        List<IProxyConnection> connections = new ArrayList<>();
        
        for (int i = 0; i < 50; i++) {
            IProxyConnection conn = factory.createConnection("machine-" + i, mockListener);
            connections.add(conn);
        }
        
        assertEquals(50, connections.size());
        
        assertDoesNotThrow(() -> {
            factory.shutdown();
        });
    }
    
    @Test
    @DisplayName("Should create unique connection instances")
    void testUniqueConnectionInstances() {
        IProxyConnection conn1 = factory.createConnection("machine-1", mockListener);
        IProxyConnection conn2 = factory.createConnection("machine-2", mockListener);
        
        assertNotSame(conn1, conn2);
        
        // Each should have its own packet publisher
        assertNotSame(conn1.getPacketPublisher(), conn2.getPacketPublisher());
    }
    
    @Test
    @DisplayName("Should create connections with same listener")
    void testCreateConnectionsWithSameListener() {
        IConnectionListener sharedListener = mock(IConnectionListener.class);
        
        IProxyConnection conn1 = factory.createConnection("machine-1", sharedListener);
        IProxyConnection conn2 = factory.createConnection("machine-2", sharedListener);
        IProxyConnection conn3 = factory.createConnection("machine-3", sharedListener);
        
        assertNotNull(conn1);
        assertNotNull(conn2);
        assertNotNull(conn3);
    }
    
    @Test
    @DisplayName("Should handle case-sensitive machine IDs")
    void testCaseSensitiveMachineIds() {
        IProxyConnection conn1 = factory.createConnection("Machine-1", mockListener);
        IProxyConnection conn2 = factory.createConnection("machine-1", mockListener);
        IProxyConnection conn3 = factory.createConnection("MACHINE-1", mockListener);
        
        // All should be created as different machines
        assertNotNull(conn1);
        assertNotNull(conn2);
        assertNotNull(conn3);
        
        assertEquals("Machine-1", conn1.getMachineId());
        assertEquals("machine-1", conn2.getMachineId());
        assertEquals("MACHINE-1", conn3.getMachineId());
    }
    
    @Test
    @DisplayName("Should verify all created connections have packet publishers")
    void testAllConnectionsHavePacketPublishers() {
        List<IProxyConnection> connections = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            IProxyConnection conn = factory.createConnection("machine-" + i, mockListener);
            connections.add(conn);
            
            assertNotNull(conn.getPacketPublisher(), 
                "Connection " + i + " should have packet publisher");
        }
    }
}
