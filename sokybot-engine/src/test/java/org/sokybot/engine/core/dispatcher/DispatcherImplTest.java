package org.sokybot.engine.core.dispatcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.DispatchException;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.network.packet.MutablePacket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DispatcherImpl.
 */
@DisplayName("DispatcherImpl Tests")
class DispatcherImplTest {
    
    private IProxyConnection mockProxyConnection;
    private DispatcherImpl dispatcher;
    
    @BeforeEach
    void setUp() {
        mockProxyConnection = mock(IProxyConnection.class);
        dispatcher = new DispatcherImpl(mockProxyConnection, "test-machine");
    }
    
    @Test
    @DisplayName("Should throw exception when proxy connection is null")
    void testNullProxyConnection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DispatcherImpl(null, "test-machine");
        });
    }
    
    @Test
    @DisplayName("Should throw exception when packet is null for sendToServer")
    void testSendToServerNullPacket() {
        assertThrows(IllegalArgumentException.class, () -> {
            dispatcher.sendToServer(null);
        });
    }
    
    @Test
    @DisplayName("Should throw exception when packet is null for sendToClient")
    void testSendToClientNullPacket() {
        assertThrows(IllegalArgumentException.class, () -> {
            dispatcher.sendToClient(null);
        });
    }
    
    @Test
    @DisplayName("Should throw DispatchException when not connected to server")
    void testSendToServerNotConnected() {
        when(mockProxyConnection.isServerConnected()).thenReturn(false);
        
        MutablePacket packet = mock(MutablePacket.class);
        
        assertThrows(DispatchException.class, () -> {
            dispatcher.sendToServer(packet);
        });
    }
    
    @Test
    @DisplayName("Should throw DispatchException when not connected to client")
    void testSendToClientNotConnected() {
        when(mockProxyConnection.isClientConnected()).thenReturn(false);
        
        MutablePacket packet = mock(MutablePacket.class);
        
        assertThrows(DispatchException.class, () -> {
            dispatcher.sendToClient(packet);
        });
    }
    
    @Test
    @DisplayName("Should successfully send packet to server")
    void testSendToServerSuccess() {
        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        
        MutablePacket packet = mock(MutablePacket.class);
        
        assertDoesNotThrow(() -> {
            dispatcher.sendToServer(packet);
        });
        
        verify(mockProxyConnection).sendToServer(packet);
    }
    
    @Test
    @DisplayName("Should successfully send packet to client")
    void testSendToClientSuccess() {
        when(mockProxyConnection.isClientConnected()).thenReturn(true);
        
        MutablePacket packet = mock(MutablePacket.class);
        
        assertDoesNotThrow(() -> {
            dispatcher.sendToClient(packet);
        });
        
        verify(mockProxyConnection).sendToClient(packet);
    }
    
    @Test
    @DisplayName("Should throw DispatchException for unsupported packet type")
    void testSendToServerUnsupportedType() {
        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        
        Object unsupportedPacket = new Object(); // Not a MutablePacket
        
        assertThrows(DispatchException.class, () -> {
            dispatcher.sendToServer(unsupportedPacket);
        });
    }
    
    @Test
    @DisplayName("Should throw DispatchException for byte array packet")
    void testSendToServerByteArrayPacket() {
        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        
        byte[] bytePacket = new byte[]{0x01, 0x02, 0x03};
        
        assertThrows(DispatchException.class, () -> {
            dispatcher.sendToServer(bytePacket);
        });
    }
    
    @Test
    @DisplayName("Should delegate isConnected to proxy connection")
    void testIsConnected() {
        when(mockProxyConnection.isConnected()).thenReturn(true);
        assertTrue(dispatcher.isConnected());
        
        when(mockProxyConnection.isConnected()).thenReturn(false);
        assertFalse(dispatcher.isConnected());
    }
    
    @Test
    @DisplayName("Should delegate isClientConnected to proxy connection")
    void testIsClientConnected() {
        when(mockProxyConnection.isClientConnected()).thenReturn(true);
        assertTrue(dispatcher.isClientConnected());
        
        when(mockProxyConnection.isClientConnected()).thenReturn(false);
        assertFalse(dispatcher.isClientConnected());
    }
    
    @Test
    @DisplayName("Should delegate isServerConnected to proxy connection")
    void testIsServerConnected() {
        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        assertTrue(dispatcher.isServerConnected());
        
        when(mockProxyConnection.isServerConnected()).thenReturn(false);
        assertFalse(dispatcher.isServerConnected());
    }
}
