package org.sokybot.engine.core.dispatcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sokybot.engine.api.DispatchException;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.LoginState;
import org.sokybot.gamemodel.ModelUpdate;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.network.NetworkPeer;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DispatcherImpl.
 */
@DisplayName("DispatcherImpl Tests")
class DispatcherImplTest {

    /** Minimal {@link IGameModel} for login pacing tests (avoids mocking OSGi-loaded interfaces). */
    private static final class StubGameModel implements IGameModel {
        private final LoginState loginState = new LoginState();

        @Override
        public LoginState getLoginState() {
            return loginState;
        }

        @Override
        public Optional<ISpawn> find(int id) {
            return Optional.empty();
        }

        @Override
        public <T extends ISpawn> Optional<T> findLive(int id, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public <T extends ISpawn> Optional<T> snapshot(int id, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public <T extends ISpawn> List<T> snapshotAll(Class<T> type) {
            return List.of();
        }

        @Override
        public Optional<ISpawn> getSelected() {
            return Optional.empty();
        }

        @Override
        public ITrainer getTrainer() {
            return null;
        }

        @Override
        public <T extends ISpawn> Flux<T> observe(int id, Class<T> type) {
            return Flux.empty();
        }

        @Override
        public <T extends ISpawn> Flux<ModelUpdate<T>> observeAll(Class<T> type) {
            return Flux.empty();
        }
    }
    
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
        MutablePacket packet = MutablePacket.wrap(new byte[] {1,2,3,4,5,6});
        
        assertDoesNotThrow(() -> {
            dispatcher.sendToServer(packet);
        });
        
        verify(mockProxyConnection).sendToServer(any(MutablePacket.class));
    }
    
    @Test
    @DisplayName("Should successfully send packet to client")
    void testSendToClientSuccess() {
        when(mockProxyConnection.isClientConnected()).thenReturn(true);
        MutablePacket packet = MutablePacket.wrap(new byte[] {1,2,3,4,5,6});
        
        assertDoesNotThrow(() -> {
            dispatcher.sendToClient(packet);
        });
        
        verify(mockProxyConnection).sendToClient(any(MutablePacket.class));
    }

    @Test
    @DisplayName("Should snapshot packet before server send")
    void testSendToServerSnapshotsPacket() {
        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        byte[] payload = new byte[] {1,2,3,4,5,6,7,8};
        MutablePacket packet = MutablePacket.wrap(payload);
        dispatcher.sendToServer(packet);
        payload[0] = 99;
        verify(mockProxyConnection).sendToServer(argThat(sent -> sent.unwrap()[0] != 99));
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

    @Test
    @DisplayName("sendLoginRequest: packet is ENCRYPTED (0x8000) so gateway security pipeline accepts 0x6102")
    void sendLoginRequest_marksEncryptedForPostHandshakeSecurity() {
        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        dispatcher.sendLoginRequest((byte) 22, "u", "p", 1, "UTF-8");
        ArgumentCaptor<MutablePacket> captor = ArgumentCaptor.forClass(MutablePacket.class);
        verify(mockProxyConnection).sendToServer(captor.capture());
        assertEquals(Encoding.ENCRYPTED, captor.getValue().getPacketEncoding());
    }

    @Test
    @DisplayName("sendAgentRequest: packet is ENCRYPTED so gateway accepts 0x6101 after handshake")
    void sendAgentRequest_marksEncryptedForPostHandshakeSecurity() {
        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        assertTrue(dispatcher.sendAgentRequest(true));
        ArgumentCaptor<MutablePacket> captor = ArgumentCaptor.forClass(MutablePacket.class);
        verify(mockProxyConnection).sendToServer(captor.capture());
        assertEquals(Encoding.ENCRYPTED, captor.getValue().getPacketEncoding());
    }

    @Test
    @DisplayName("sendLoginRequest: locale 0 coerces to 22 on wire (byte index 6 after header)")
    void sendLoginRequest_localeZero_coercesToVsroLocaleOnWire() {
        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        dispatcher.sendLoginRequest((byte) 0, "admin", "123123123", 3, "UTF-8");
        ArgumentCaptor<MutablePacket> captor = ArgumentCaptor.forClass(MutablePacket.class);
        verify(mockProxyConnection).sendToServer(captor.capture());
        byte[] raw = captor.getValue().unwrap();
        assertTrue(raw.length > 6, "packet must include 6-byte header + body");
        assertEquals(0x16, raw[6] & 0xFF,
                "first payload byte must be 0x16 (locale 22); if capture shows 0x00, runtime is not using this DispatcherImpl");
    }

    @Test
    @DisplayName("sendLoginRequest: duplicate calls delegate to proxy (0x6102 throttle lives in proxy pipeline)")
    void sendLoginRequest_duplicateCalls_forwardBothToProxy() {
        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        dispatcher.sendLoginRequest((byte) 22, "u", "p", 1, "UTF-8");
        dispatcher.sendLoginRequest((byte) 22, "u", "p", 1, "UTF-8");
        verify(mockProxyConnection, times(2)).sendToServer(any());
    }

    @Test
    @DisplayName("sendLoginRequest: sleeps until pause elapsed since agent list observed wall clock")
    void sendLoginRequest_pacesFromAgentListWallClock() {
        IWorkflowContext ctx = mock(IWorkflowContext.class);
        Map<String, Object> persistent = new HashMap<>();
        Map<String, Object> loginSnap = new HashMap<>();
        loginSnap.put("gatewayLoginPauseAfterAgentListMs", 250L);
        persistent.put("runtimeLoginSettings", loginSnap);
        when(ctx.getPersistentData()).thenReturn(persistent);

        StubGameModel gameModel = new StubGameModel();
        gameModel.getLoginState().setLastGatewayAgentListObservedWallClockMs(
                System.currentTimeMillis() - 40L);

        DispatcherImpl d = new DispatcherImpl(mockProxyConnection, "m1", gameModel);
        d.bindWorkflowContext(ctx);
        when(mockProxyConnection.isServerConnected()).thenReturn(true);

        long t0 = System.currentTimeMillis();
        d.sendLoginRequest((byte) 22, "u", "p", 1, "UTF-8");
        long elapsed = System.currentTimeMillis() - t0;
        assertTrue(elapsed >= 200L, "expected ~210ms pacing (250-40), elapsed=" + elapsed);
        verify(mockProxyConnection).sendToServer(any());
        assertEquals(0L, gameModel.getLoginState().getLastGatewayAgentListObservedWallClockMs());
    }

    @Test
    @DisplayName("sendLoginRequest: uses proxy 0xA101 wall clock when game model stamp is absent")
    void sendLoginRequest_pacesFromProxyWallClock() {
        IWorkflowContext ctx = mock(IWorkflowContext.class);
        Map<String, Object> persistent = new HashMap<>();
        Map<String, Object> loginSnap = new HashMap<>();
        loginSnap.put("gatewayLoginPauseAfterAgentListMs", 300L);
        persistent.put("runtimeLoginSettings", loginSnap);
        when(ctx.getPersistentData()).thenReturn(persistent);

        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        when(mockProxyConnection.getLastGatewayAgentListFromServerWallClockMs())
                .thenReturn(System.currentTimeMillis() - 55L);

        DispatcherImpl d = new DispatcherImpl(mockProxyConnection, "m1", null);
        d.bindWorkflowContext(ctx);

        long t0 = System.currentTimeMillis();
        d.sendLoginRequest((byte) 22, "u", "p", 1, "UTF-8");
        long elapsed = System.currentTimeMillis() - t0;
        assertTrue(elapsed >= 230L, "expected ~245ms pacing (300-55), elapsed=" + elapsed);
        verify(mockProxyConnection).sendToServer(any());
        verify(mockProxyConnection).clearLastGatewayAgentListFromServerWallClockMs();
    }

    @Test
    @DisplayName("sendToServer: legacy 0x6102 path forwards each call to proxy (throttle in proxy pipeline)")
    void sendToServer_duplicateLoginOpcode_forwardsBothToProxy() {
        when(mockProxyConnection.isServerConnected()).thenReturn(true);
        MutablePacket p = MutablePacket.getBuilder(9, ClientOpcode.LOGIN_REQUEST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 22)
                .putShort((short) 1)
                .putBytes(new byte[] {'u'})
                .putShort((short) 1)
                .putBytes(new byte[] {'p'})
                .putShort((short) 1)
                .build();
        dispatcher.sendToServer(p);
        dispatcher.sendToServer(p);
        verify(mockProxyConnection, times(2)).sendToServer(any());
    }
}
